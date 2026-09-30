package com.project.englishlearning.controller;

import com.project.englishlearning.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CourseService courseService;
    private final com.project.englishlearning.repository.UserRepository userRepository;

    public HomeController(CourseService courseService, com.project.englishlearning.repository.UserRepository userRepository) {
        this.courseService = courseService;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String home(Model model, org.springframework.security.core.Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/admin/dashboard";
        }

        Long userId = null;
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            com.project.englishlearning.entity.User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                userId = user.getId();
            }
        }

        if (userId != null) {
            model.addAttribute("courses", courseService.getAllCoursesDTOForUser(userId));
        } else {
            model.addAttribute("courses", courseService.getAllCoursesDTO());
        }

        return "index";
    }
}