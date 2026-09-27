package com.avms.audit;

import com.avms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import jakarta.persistence.Index;

@Entity
@Table(
    name = "AUDIT_AUDITLOG",
    indexes = {@Index(name = "idx_audit_module", columnList = "module"), @Index(name = "idx_audit_created", columnList = "created_at")})
public class AuditLog extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_email", length = 254)
  private String userEmail;

  @Column(name = "action", nullable = false, length = 40)
  private String action;

  @Column(name = "module", nullable = false, length = 60)
  private String module;

  @Column(name = "object_type", length = 80)
  private String objectType;

  @Column(name = "object_id", length = 80)
  private String objectId;

  @Column(name = "ip_address", length = 45)
  private String ipAddress;

  @Column(name = "description", length = 1000)
  private String description;

  public AuditLog() {}

  public AuditLog(String userEmail, String action, String module, String objectType, String objectId, String ipAddress, String description) {
    this.userEmail = userEmail;
    this.action = action;
    this.module = module;
    this.objectType = objectType;
    this.objectId = objectId;
    this.ipAddress = ipAddress;
    this.description = description;
  }

  public Long getId() { return id; }
  public String getUserEmail() { return userEmail; }
  public String getAction() { return action; }
  public String getModule() { return module; }
  public String getObjectType() { return objectType; }
  public String getObjectId() { return objectId; }
  public String getIpAddress() { return ipAddress; }
  public String getDescription() { return description; }
}
