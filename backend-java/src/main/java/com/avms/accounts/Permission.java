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

@Entity
@Table(name = "ACCOUNTS_PERMISSION")
public class Permission {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "code", nullable = false, unique = true, length = 80)
  private String code;

  @Column(name = "name", nullable = false, length = 120)
  private String name;

  @Column(name = "module", nullable = false, length = 60)
  private String module;

  public Permission() {}

  public Permission(String code, String name, String module) {
    this.code = code;
    this.name = name;
    this.module = module;
  }

  public Long getId() { return id; }
  public String getCode() { return code; }
  public String getName() { return name; }
  public String getModule() { return module; }
}
