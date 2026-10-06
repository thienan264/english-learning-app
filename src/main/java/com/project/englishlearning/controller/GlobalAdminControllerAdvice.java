package com.project.englishlearning.controller;

import com.project.englishlearning.repository.AdminNotificationRepository;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages = "com.project.englishlearning.controller")
public class GlobalAdminControllerAdvice {

    private final AdminNotificationRepository notificationRepo;

    public GlobalAdminControllerAdvice(AdminNotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @ModelAttribute
    public void addNotificationsToModel(HttpServletRequest request, Model model) {
        if (request.getRequestURI() != null && request.getRequestURI().startsWith("/admin")) {
            model.addAttribute("adminUnreadNotifs", notificationRepo.findByIsReadFalseOrderByCreatedAtDesc());
            model.addAttribute("adminUnreadCount", notificationRepo.countByIsReadFalse());
        }
    }
}

