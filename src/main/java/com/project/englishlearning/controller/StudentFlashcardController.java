package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.service.FlashcardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/courses/{courseId}/study-flashcards")
public class StudentFlashcardController {

    private final FlashcardService flashcardService;
    private final CourseRepository courseRepository;

    public StudentFlashcardController(FlashcardService flashcardService, CourseRepository courseRepository) {
        this.flashcardService = flashcardService;
        this.courseRepository = courseRepository;
    }

    @GetMapping
    public String studyFlashcards(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return "redirect:/"; // Nếu khóa học không tồn tại thì đẩy về trang chủ
        }

        model.addAttribute("course", course);
        model.addAttribute("flashcards", flashcardService.getFlashcardsByCourseId(courseId));
        return "student/flashcard-study";
    }
}