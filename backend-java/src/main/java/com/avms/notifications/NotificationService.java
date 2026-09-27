package com.avms.notifications;

import com.avms.accounts.AccountsUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Port of Django notifications.services: single notify + venture fan-out by permission. */
@Service
public class NotificationService {

  private final NotificationRepository notifications;
  private final AccountsUserRepository users;

  public NotificationService(NotificationRepository notifications, AccountsUserRepository users) {
    this.notifications = notifications;
    this.users = users;
  }

  @Transactional
  public void notify(String userEmail, String type, String message, String link) {
    notifications.save(new Notification(userEmail, type, message, link));
  }

  /**
   * Fan-out to active superusers + holders of permCode (mirrors notify_venture_users).
   * Venture filtering of users is applied by callers via user-venture association when
   * available; Django's version notifies all holders globally, so parity is global fan-out.
   */
  @Transactional
  public int notifyByPermission(String type, String message, String permCode, String link) {
    int count = 0;
    for (var user : users.findByIsActiveTrue()) {
      boolean superuser = Boolean.TRUE.equals(user.getIsSuperuser());
      if (superuser || user.hasPermissionCode(permCode)) {
        notifications.save(new Notification(user.getEmail(), type, message, link));
        count++;
      }
    }
    return count;
  }
}
