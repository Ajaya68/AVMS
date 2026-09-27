package com.avms.accounts;

import com.avms.audit.AuditService;
import com.avms.common.ForbiddenException;
import com.avms.common.ResourceNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django UserListCreateView/UserDetailView incl. superuser/self-delete guards. */
@Service
public class UserService {

  private final AccountsUserRepository users;
  private final RoleRepository roles;
  private final PasswordEncoder encoder;
  private final AuditService audit;

  public UserService(AccountsUserRepository users, RoleRepository roles, PasswordEncoder encoder, AuditService audit) {
    this.users = users;
    this.roles = roles;
    this.encoder = encoder;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public Page<AuthDtos.UserResponse> list(String search, Pageable pageable) {
    Page<AccountsUser> page;
    if (search != null && !search.isBlank()) {
      List<AccountsUser> matched = users.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(search, search);
      page = new org.springframework.data.domain.PageImpl<>(matched, pageable,
          matched.size() <= pageable.getPageSize() ? matched.size() : matched.size());
    } else {
      page = users.findAll(pageable);
    }
    return page.map(AuthService::toUserResponse);
  }

  @Transactional
  public AuthDtos.UserResponse create(AuthDtos.UserCreateRequest req) {
    String email = req.email().toLowerCase().trim();
    if (users.findByEmailIgnoreCase(email).isPresent()) {
      throw new IllegalArgumentException("User with this email already exists.");
    }
    AccountsUser user = new AccountsUser(email, encoder.encode(req.password()));
    user.setFullName(req.fullName());
    user.setPhone(req.phone());
    user.setRoles(resolveRoles(req.roles()));
    users.save(user);
    audit.log("USER_CREATED", "accounts", "User", String.valueOf(user.getId()), "User " + email + " created.");
    return AuthService.toUserResponse(user);
  }

  @Transactional(readOnly = true)
  public AuthDtos.UserResponse get(Long id) {
    return AuthService.toUserResponse(users.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found.")));
  }

  @Transactional
  public AuthDtos.UserResponse update(Long id, AuthDtos.UserUpdateRequest req) {
    AccountsUser user = users.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    if (req.fullName() != null) {
      user.setFullName(req.fullName());
    }
    if (req.phone() != null) {
      user.setPhone(req.phone());
    }
    if (req.isActive() != null) {
      user.setIsActive(req.isActive());
    }
    if (req.roles() != null) {
      user.setRoles(resolveRoles(req.roles()));
    }
    users.save(user);
    audit.log("UPDATE", "accounts", "User", String.valueOf(id), "User " + user.getEmail() + " updated.");
    return AuthService.toUserResponse(user);
  }

  @Transactional
  public void delete(Long id, String currentEmail) {
    AccountsUser user = users.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    if (Boolean.TRUE.equals(user.getIsSuperuser())) {
      throw new ForbiddenException("Superuser accounts cannot be deleted.");
    }
    if (user.getEmail().equalsIgnoreCase(currentEmail)) {
      throw new ForbiddenException("You cannot delete your own account.");
    }
    users.delete(user);
    audit.log("DELETE", "accounts", "User", String.valueOf(id), "User " + user.getEmail() + " deleted.");
  }

  private Set<Role> resolveRoles(Set<String> codes) {
    Set<Role> out = new HashSet<>();
    if (codes == null) {
      return out;
    }
    for (String code : codes) {
      roles.findByCode(code).ifPresent(r -> {
        if (r.isActive()) {
          out.add(r);
        }
      });
    }
    return out;
  }
}
