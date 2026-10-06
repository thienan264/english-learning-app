package com.project.englishlearning.controller;

import com.project.englishlearning.entity.AdminNotification;
import com.project.englishlearning.repository.AdminNotificationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private final AdminNotificationRepository notificationRepo;

    public AdminNotificationController(AdminNotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @GetMapping
    public String listNotifications(Model model) {
        List<AdminNotification> notifications = notificationRepo.findAllByOrderByCreatedAtDesc();
        model.addAttribute("notifications", notifications);
        return "admin/notification-list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id, @RequestParam(required = false) String redirectUrl) {
        AdminNotification notification = notificationRepo.findById(id).orElse(null);
        if (notification != null) {
            notification.setIsRead(true);
            notificationRepo.save(notification);
            if (notification.getLink() != null && !notification.getLink().isEmpty()) {
                return "redirect:" + notification.getLink();
            }
        }
        if (redirectUrl != null) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/admin/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(RedirectAttributes redirectAttributes) {
        List<AdminNotification> unread = notificationRepo.findByIsReadFalseOrderByCreatedAtDesc();
        for (AdminNotification n : unread) {
            n.setIsRead(true);
        }
        notificationRepo.saveAll(unread);
        redirectAttributes.addFlashAttribute("successMsg", "Đã đánh dấu tất cả là đã đọc.");
        return "redirect:/admin/notifications";
    }
}

