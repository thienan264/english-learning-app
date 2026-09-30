package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Flashcard;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.entity.UserFlashcardProgress;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.repository.UserFlashcardProgressRepository;
import com.project.englishlearning.service.FlashcardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses/{courseId}/study-flashcards")
public class StudentFlashcardController {

    private final FlashcardService flashcardService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserFlashcardProgressRepository progressRepository;

    public StudentFlashcardController(FlashcardService flashcardService, 
                                      CourseRepository courseRepository,
                                      UserRepository userRepository,
                                      UserFlashcardProgressRepository progressRepository) {
        this.flashcardService = flashcardService;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.progressRepository = progressRepository;
    }

    @GetMapping
    public String studyFlashcards(@PathVariable Long courseId, Model model, Authentication auth) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return "redirect:/"; 
        }

        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        List<Flashcard> flashcards = flashcardService.getFlashcardsByCourseId(courseId);
        
        List<UserFlashcardProgress> progresses = progressRepository.findByUserIdAndFlashcardCourseId(user.getId(), courseId);
        Map<Long, Boolean> flippedMap = progresses.stream()
                .collect(Collectors.toMap(p -> p.getFlashcard().getId(), UserFlashcardProgress::getIsFlipped));

        long flippedCount = progresses.stream().filter(p -> Boolean.TRUE.equals(p.getIsFlipped())).count();
        boolean allCompleted = (flippedCount >= flashcards.size() && !flashcards.isEmpty());

        model.addAttribute("course", course);
        model.addAttribute("flashcards", flashcards);
        model.addAttribute("flippedMap", flippedMap);
        model.addAttribute("allCompleted", allCompleted);
        return "student/flashcard-study";
    }
}
