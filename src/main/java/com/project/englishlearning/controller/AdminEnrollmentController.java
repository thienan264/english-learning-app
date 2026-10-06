package com.project.englishlearning.controller;

import com.project.englishlearning.entity.UserCourseEnrollment;
import com.project.englishlearning.repository.UserCourseEnrollmentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/enrollments")
public class AdminEnrollmentController {

    private final UserCourseEnrollmentRepository enrollmentRepo;

    public AdminEnrollmentController(UserCourseEnrollmentRepository enrollmentRepo) {
        this.enrollmentRepo = enrollmentRepo;
    }

    @GetMapping
    public String listEnrollments(Model model) {
        List<UserCourseEnrollment> enrollments = enrollmentRepo.findAll();
        model.addAttribute("enrollments", enrollments);
        return "admin/enrollment-list";
    }

    @PostMapping("/{id}/revoke")
    public String revokeEnrollment(@PathVariable Long id, @RequestParam(required = false) Long userId, RedirectAttributes redirectAttributes) {
        UserCourseEnrollment enrollment = enrollmentRepo.findById(id).orElse(null);
        if (enrollment != null) {
            enrollment.setStatus("REVOKED");
            enrollmentRepo.save(enrollment);
            redirectAttributes.addFlashAttribute("successMsg", "Đã thu hồi quyền học thành công!");
        }
        if (userId != null) {
            return "redirect:/admin/users/" + userId;
        }
        return "redirect:/admin/enrollments";
    }
    
    @PostMapping("/{id}/extend")
    public String extendEnrollment(@PathVariable Long id, @RequestParam("months") int months, @RequestParam(required = false) Long userId, RedirectAttributes redirectAttributes) {
        UserCourseEnrollment enrollment = enrollmentRepo.findById(id).orElse(null);
        if (enrollment != null) {
            if (enrollment.getExpiresAt() != null) {
                if (enrollment.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                    enrollment.setExpiresAt(java.time.LocalDateTime.now().plusMonths(months));
                } else {
                    enrollment.setExpiresAt(enrollment.getExpiresAt().plusMonths(months));
                }
            } else {
                enrollment.setExpiresAt(java.time.LocalDateTime.now().plusMonths(months));
            }
            if ("REVOKED".equals(enrollment.getStatus())) {
                enrollment.setStatus("IN_PROGRESS");
            }
            enrollmentRepo.save(enrollment);
            redirectAttributes.addFlashAttribute("successMsg", "Đã gia hạn thành công " + months + " tháng!");
        }
        if (userId != null) {
            return "redirect:/admin/users/" + userId;
        }
        return "redirect:/admin/enrollments";
    }
}
