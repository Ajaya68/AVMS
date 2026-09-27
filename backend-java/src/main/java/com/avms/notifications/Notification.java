package com.avms.notifications;

import com.avms.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

@Entity
@Table(name = "NOTIFICATIONS_NOTIFICATION")
public class Notification extends BaseEntity {

  @jakarta.persistence.Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_email", nullable = false, length = 254)
  private String userEmail;

  @Column(name = "type", nullable = false, length = 30)
  private String type;

  @Column(name = "message", nullable = false, length = 255)
  private String message;

  @Column(name = "link", length = 255)
  private String link;

  @Column(name = "is_read", nullable = false)
  private boolean read;

  public Notification() {}

  public Notification(String userEmail, String type, String message, String link) {
    this.userEmail = userEmail;
    this.type = type;
    this.message = message;
    this.link = link;
  }

  public Long getId() { return id; }
  public String getUserEmail() { return userEmail; }
  public String getType() { return type; }
  public String getMessage() { return message; }
  public String getLink() { return link; }
  public boolean isRead() { return read; }
  public void setRead(boolean read) { this.read = read; }
}
