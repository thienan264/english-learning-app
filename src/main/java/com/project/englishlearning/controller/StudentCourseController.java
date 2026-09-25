package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.service.LessonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/courses")
public class StudentCourseController {

    private final CourseRepository courseRepository;
    private final LessonService lessonService;

    public StudentCourseController(CourseRepository courseRepository, LessonService lessonService) {
        this.courseRepository = courseRepository;
        this.lessonService = lessonService;
    }

    @GetMapping("/{id}")
    public String courseDetails(@PathVariable Long id, Model model) {
        Course course = courseRepository.findById(id).orElseThrow();
        
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessonService.getLessonsByCourseId(id));
        return "student/course-details";
    }
}