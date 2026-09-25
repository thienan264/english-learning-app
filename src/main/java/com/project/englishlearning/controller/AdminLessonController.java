package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.service.CourseService;
import com.project.englishlearning.service.LessonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/courses/{courseId}/lessons")
public class AdminLessonController {

    private final LessonService lessonService;
    private final CourseService courseService;

    public AdminLessonController(LessonService lessonService, CourseService courseService) {
        this.lessonService = lessonService;
        this.courseService = courseService;
    }

    @GetMapping
    public String listLessons(@PathVariable Long courseId, Model model) {
        Course course = courseService.getCourseById(courseId);
        if (course == null) return "redirect:/admin/courses";
        
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessonService.getLessonsByCourseId(courseId));
        return "admin/lesson-list";
    }

    @PostMapping("/add")
    public String addLesson(@PathVariable Long courseId, @ModelAttribute Lesson lesson) {
        Course course = courseService.getCourseById(courseId);
        if (course != null) {
            lesson.setCourse(course); // Gắn bài học này vào đúng khóa học
            lessonService.saveLesson(lesson);
        }
        return "redirect:/admin/courses/" + courseId + "/lessons";
    }
}