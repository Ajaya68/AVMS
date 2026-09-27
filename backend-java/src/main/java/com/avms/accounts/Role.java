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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "ACCOUNTS_ROLE")
public class Role extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 40)
  private String code;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @ManyToMany
  @JoinTable(
      name = "ACCOUNTS_ROLE_PERMISSIONS",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  private Set<Permission> permissions = new HashSet<>();

  public Role() {}

  public Role(String code, String name, String description) {
    this.code = code;
    this.name = name;
    this.description = description;
  }

  public Long getId() { return id; }
  public String getCode() { return code; }
  public String getName() { return name; }
  public String getDescription() { return description; }
  public boolean isActive() { return active; }
  public void setActive(boolean active) { this.active = active; }
  public Set<Permission> getPermissions() { return permissions; }
  public void setPermissions(Set<Permission> permissions) { this.permissions = permissions; }

  public Set<String> permissionCodes() {
    return permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
  }
}
