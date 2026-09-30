package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.service.CourseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/courses")
public class AdminCourseController {

    private final CourseService courseService;

    public AdminCourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        return "admin/course-list"; 
    }

    @PostMapping("/add")
    public String addCourse(@ModelAttribute Course course) {
        courseService.saveCourse(course);
        return "redirect:/admin/courses"; 
    }

    @PostMapping("/edit/{id}")
    public String editCourse(@PathVariable Long id, @ModelAttribute Course updatedCourse) {
        Course existingCourse = courseService.getCourseById(id);
        existingCourse.setTitle(updatedCourse.getTitle());
        existingCourse.setLevel(updatedCourse.getLevel());
        existingCourse.setDescription(updatedCourse.getDescription());
        existingCourse.setThumbnailUrl(updatedCourse.getThumbnailUrl());
        courseService.saveCourse(existingCourse);
        return "redirect:/admin/courses";
    }

    @PostMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return "redirect:/admin/courses";
    }
}
