package com.avms.accounts;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import com.avms.security.CustomUserDetails;
import com.avms.security.PermEvaluator;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService auth;
  private final UserService userService;
  private final RoleRepository roleRepository;
  private final PermEvaluator perm;

  public AuthController(AuthService auth, UserService userService, RoleRepository roleRepository, PermEvaluator perm) {
    this.auth = auth;
    this.userService = userService;
    this.roleRepository = roleRepository;
    this.perm = perm;
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
    return ResponseEntity.ok(ApiResponse.ok(auth.login(req.email(), req.password())));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(@Valid @RequestBody AuthDtos.RefreshRequest req) {
    return ResponseEntity.ok(ApiResponse.ok(auth.refresh(req.refresh())));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Map<String, String>>> logout(@Valid @RequestBody AuthDtos.LogoutRequest req) {
    auth.logout(req.refresh());
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Logged out.")));
  }

  @GetMapping("/me")
  public ResponseEntity<ApiResponse<AuthDtos.UserResponse>> me(Authentication authentication) {
    String email = ((CustomUserDetails) authentication.getPrincipal()).getEmail();
    return ResponseEntity.ok(ApiResponse.ok(auth.me(email)));
  }

  @PostMapping("/change-password")
  public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
      @Valid @RequestBody AuthDtos.ChangePasswordRequest req, Authentication authentication) {
    String email = ((CustomUserDetails) authentication.getPrincipal()).getEmail();
    auth.changePassword(email, req.currentPassword(), req.newPassword());
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Password changed.")));
  }

  @GetMapping("/users")
  public ResponseEntity<ApiResponse<PageResponse<AuthDtos.UserResponse>>> listUsers(
      @RequestParam(required = false) String search, Pageable pageable) {
    perm.require("users.manage");
    var page = userService.list(search, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @PostMapping("/users")
  public ResponseEntity<ApiResponse<AuthDtos.UserResponse>> createUser(@Valid @RequestBody AuthDtos.UserCreateRequest req) {
    perm.require("users.manage");
    return ResponseEntity.status(201).body(ApiResponse.ok(userService.create(req), "User created."));
  }

  @GetMapping("/users/{id}")
  public ResponseEntity<ApiResponse<AuthDtos.UserResponse>> getUser(@PathVariable Long id) {
    perm.require("users.manage");
    return ResponseEntity.ok(ApiResponse.ok(userService.get(id)));
  }

  @PatchMapping("/users/{id}")
  public ResponseEntity<ApiResponse<AuthDtos.UserResponse>> updateUser(
      @PathVariable Long id, @RequestBody AuthDtos.UserUpdateRequest req) {
    perm.require("users.manage");
    return ResponseEntity.ok(ApiResponse.ok(userService.update(id, req), "User updated."));
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<ApiResponse<Map<String, String>>> deleteUser(@PathVariable Long id, Authentication authentication) {
    perm.require("users.manage");
    String email = ((CustomUserDetails) authentication.getPrincipal()).getEmail();
    userService.delete(id, email);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "User deleted.")));
  }

  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<java.util.List<AuthDtos.RoleResponse>>> listRoles() {
    perm.require("users.manage");
    var out = roleRepository.findByActiveTrue().stream()
        .map(r -> new AuthDtos.RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.permissionCodes()))
        .toList();
    return ResponseEntity.ok(ApiResponse.ok(out));
  }
}
