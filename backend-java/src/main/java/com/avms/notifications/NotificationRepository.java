package com.avms.notifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  Page<Notification> findByUserEmailOrderByCreatedAtDescIdDesc(String userEmail, Pageable pageable);
  long countByUserEmailAndReadFalse(String userEmail);
}
