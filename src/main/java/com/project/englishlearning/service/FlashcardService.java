package com.project.englishlearning.service;

import com.project.englishlearning.entity.Flashcard;
import java.util.List;

public interface FlashcardService {
    List<Flashcard> getFlashcardsByCourseId(Long courseId);
    Flashcard saveFlashcard(Long courseId, Flashcard flashcard);
}