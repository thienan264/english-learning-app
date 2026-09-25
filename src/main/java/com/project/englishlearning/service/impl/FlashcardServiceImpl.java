package com.project.englishlearning.service.impl;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Flashcard;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.repository.FlashcardRepository;
import com.project.englishlearning.service.FlashcardService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FlashcardServiceImpl implements FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final CourseRepository courseRepository;

    public FlashcardServiceImpl(FlashcardRepository flashcardRepository, CourseRepository courseRepository) {
        this.flashcardRepository = flashcardRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public List<Flashcard> getFlashcardsByCourseId(Long courseId) {
        return flashcardRepository.findByCourseId(courseId);
    }

    @Override
    public Flashcard saveFlashcard(Long courseId, Flashcard flashcard) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        flashcard.setCourse(course);
        return flashcardRepository.save(flashcard);
    }
}