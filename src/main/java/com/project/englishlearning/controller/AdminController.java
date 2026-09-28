package com.project.englishlearning.controller;

import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final TestResultRepository testResultRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;

    public AdminController(UserRepository userRepository, CourseRepository courseRepository,
                           LessonRepository lessonRepository, TestResultRepository testResultRepository,
                           WritingSubmissionRepository writingSubmissionRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.testResultRepository = testResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCourses", courseRepository.count());
        model.addAttribute("totalLessons", lessonRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalTests", testResultRepository.count());
        model.addAttribute("totalWritings", writingSubmissionRepository.count());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/user-list";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElseThrow();
        if ("ACTIVE".equals(user.getStatus())) {
            user.setStatus("LOCKED");
        } else {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái tài khoản thành công.");
        return "redirect:/admin/users";
    }
}