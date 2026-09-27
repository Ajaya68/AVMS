package com.avms.notifications;

import com.avms.common.ApiResponse;
import com.avms.common.PageResponse;
import com.avms.common.ResourceNotFoundException;
import com.avms.security.CustomUserDetails;
import com.avms.security.PermEvaluator;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationRepository repository;
  private final PermEvaluator perm;

  public NotificationController(NotificationRepository repository, PermEvaluator perm) {
    this.repository = repository;
    this.perm = perm;
  }

  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<Notification>>> list(Authentication auth, Pageable pageable) {
    perm.require("notifications.view");
    String email = ((CustomUserDetails) auth.getPrincipal()).getEmail();
    var page = repository.findByUserEmailOrderByCreatedAtDescIdDesc(email, pageable);
    return ResponseEntity.ok(ApiResponse.ok(PageResponse.of(page.getTotalElements(), page.getContent())));
  }

  @GetMapping("/unread-count")
  public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(Authentication auth) {
    perm.require("notifications.view");
    String email = ((CustomUserDetails) auth.getPrincipal()).getEmail();
    return ResponseEntity.ok(ApiResponse.ok(Map.of("count", repository.countByUserEmailAndReadFalse(email))));
  }

  @PostMapping("/{id}/read")
  @Transactional
  public ResponseEntity<ApiResponse<Map<String, String>>> markRead(
      @PathVariable Long id, Authentication auth) {
    perm.require("notifications.view");
    String email = ((CustomUserDetails) auth.getPrincipal()).getEmail();
    Notification notification = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Notification not found."));
    if (!notification.getUserEmail().equalsIgnoreCase(email)) {
      throw new ResourceNotFoundException("Notification not found.");
    }
    notification.setRead(true);
    repository.save(notification);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("detail", "Marked as read.")));
  }

  @PostMapping("/read-all")
  @Transactional
  public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(Authentication auth, Pageable pageable) {
    perm.require("notifications.view");
    String email = ((CustomUserDetails) auth.getPrincipal()).getEmail();
    var page = repository.findByUserEmailOrderByCreatedAtDescIdDesc(email, pageable);
    int count = 0;
    for (Notification n : page.getContent()) {
      if (!n.isRead()) {
        n.setRead(true);
        repository.save(n);
        count++;
      }
    }
    return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
  }
}
