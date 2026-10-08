package com.project.englishlearning.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/** Old flat lesson manager is replaced by the course curriculum builder. */
@Controller
@RequestMapping("/admin/courses/{courseId}/lessons")
public class AdminLessonController {
    @GetMapping
    public String listLessons(@PathVariable Long courseId) {
        return "redirect:/admin/courses/"+courseId+"/builder";
    }
    // An old open browser tab must not create lessons outside the curriculum.
    @PostMapping("/add")
    public String retiredAddLesson(@PathVariable Long courseId) {
        return "redirect:/admin/courses/"+courseId+"/builder";
    }
}
