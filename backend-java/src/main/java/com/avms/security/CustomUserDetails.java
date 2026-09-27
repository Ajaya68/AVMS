package com.avms.security;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class CustomUserDetails {
  private final String email;
  private final boolean superuser;
  private final Collection<SimpleGrantedAuthority> authorities;

  public CustomUserDetails(String email, boolean superuser, Collection<SimpleGrantedAuthority> authorities) {
    this.email = email;
    this.superuser = superuser;
    this.authorities = authorities;
  }

  public String getEmail() {
    return email;
  }

  public boolean isSuperuser() {
    return superuser;
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  public boolean hasPermission(String code) {
    if (superuser) {
      return true;
    }
    return authorities.contains(new SimpleGrantedAuthority("PERM_" + code));
  }

  public Set<String> permissionCodes() {
    java.util.HashSet<String> out = new java.util.HashSet<>();
    for (GrantedAuthority a : authorities) {
      String s = a.getAuthority();
      if (s.startsWith("PERM_")) {
        out.add(s.substring(5));
      }
    }
    return out;
  }
}
