package com.project.englishlearning.repository;

import com.project.englishlearning.entity.Flashcard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    List<Flashcard> findByCourseId(Long courseId);
}

