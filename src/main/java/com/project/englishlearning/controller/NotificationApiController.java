package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Notification;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.NotificationRepository;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationApiController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Notification> list = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("notifications", list);
        response.put("unreadCount", unreadCount);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return ResponseEntity.status(401).build();

        Notification notif = notificationRepository.findById(id).orElse(null);
        if (notif != null && notif.getUser().getUsername().equals(auth.getName())) {
            notif.setIsRead(true);
            notificationRepository.save(notif);
            return ResponseEntity.ok(Map.of("success", true, "url", notif.getUrl()));
        }
        return ResponseEntity.notFound().build();
    }
}
