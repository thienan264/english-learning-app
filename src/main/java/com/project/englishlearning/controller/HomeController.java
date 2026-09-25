package com.project.englishlearning.controller;

import com.project.englishlearning.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final CourseService courseService;

    public HomeController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/")
    public String home(Model model) {
        // Lấy tất cả khóa học và gửi ra giao diện
        model.addAttribute("courses", courseService.getAllCourses());
        return "index";
    }
}