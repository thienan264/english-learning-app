package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Flashcard;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.service.FlashcardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/courses/{courseId}/flashcards")
public class AdminFlashcardController {

    private final FlashcardService flashcardService;
    private final CourseRepository courseRepository;

    public AdminFlashcardController(FlashcardService flashcardService, CourseRepository courseRepository) {
        this.flashcardService = flashcardService;
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public String listFlashcards(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) return "redirect:/admin/courses";

        model.addAttribute("course", course);
        model.addAttribute("flashcards", flashcardService.getFlashcardsByCourseId(courseId));
        return "admin/flashcard-list";
    }

    @PostMapping("/add")
    public String addFlashcard(@PathVariable Long courseId, @ModelAttribute Flashcard flashcard) {
        flashcardService.saveFlashcard(courseId, flashcard);
        return "redirect:/admin/courses/" + courseId + "/flashcards";
    }
}