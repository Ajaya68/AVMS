package com.avms.accounts;

import com.avms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Table(name = "ACCOUNTS_USER")
public class AccountsUser extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "email", nullable = false, unique = true, length = 254)
  private String email;

  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @Column(name = "full_name", length = 150)
  private String fullName;

  @Column(name = "phone", length = 30)
  private String phone;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "is_staff", nullable = false)
  private Boolean isStaff = false;

  @Column(name = "is_superuser", nullable = false)
  private Boolean isSuperuser = false;

  @Column(name = "last_login")
  private Instant lastLogin;

  @ManyToMany
  @JoinTable(
      name = "ACCOUNTS_USER_ROLES",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles = new HashSet<>();

  public AccountsUser() {}

  public AccountsUser(String email, String password) {
    this.email = email.toLowerCase();
    this.password = password;
  }

  public Long getId() { return id; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email.toLowerCase(); }
  public String getPassword() { return password; }
  public void setPassword(String password) { this.password = password; }
  public String getFullName() { return fullName; }
  public void setFullName(String fullName) { this.fullName = fullName; }
  public String getPhone() { return phone; }
  public void setPhone(String phone) { this.phone = phone; }
  public Boolean getIsActive() { return isActive; }
  public void setIsActive(Boolean isActive) { this.isActive = isActive; }
  public Boolean getIsStaff() { return isStaff; }
  public void setIsStaff(Boolean isStaff) { this.isStaff = isStaff; }
  public Boolean getIsSuperuser() { return isSuperuser; }
  public void setIsSuperuser(Boolean isSuperuser) { this.isSuperuser = isSuperuser; }
  public Instant getLastLogin() { return lastLogin; }
  public void setLastLogin(Instant lastLogin) { this.lastLogin = lastLogin; }
  public Set<Role> getRoles() { return roles; }
  public void setRoles(Set<Role> roles) { this.roles = roles; }

  public Set<String> roleCodes() {
    Set<String> out = new TreeSet<>();
    for (Role r : roles) {
      if (r.isActive()) {
        out.add(r.getCode());
      }
    }
    return out;
  }

  public Set<String> permissionCodes() {
    Set<String> out = new TreeSet<>();
    for (Role r : roles) {
      if (r.isActive()) {
        out.addAll(r.permissionCodes());
      }
    }
    return out;
  }

  public boolean hasPermissionCode(String code) {
    if (Boolean.TRUE.equals(isSuperuser)) {
      return true;
    }
    return permissionCodes().contains(code);
  }
}
