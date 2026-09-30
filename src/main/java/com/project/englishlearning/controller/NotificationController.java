package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Notification;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.NotificationRepository;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String viewAllNotifications(Authentication auth, Model model) {
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        // Count unread
        long unreadCount = notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        
        return "student/notifications";
    }
}
