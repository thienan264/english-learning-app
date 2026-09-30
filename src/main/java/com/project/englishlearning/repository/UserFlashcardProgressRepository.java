package com.project.englishlearning.repository;

import com.project.englishlearning.entity.UserFlashcardProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserFlashcardProgressRepository extends JpaRepository<UserFlashcardProgress, Long> {
    Optional<UserFlashcardProgress> findByUserIdAndFlashcardId(Long userId, Long flashcardId);
    List<UserFlashcardProgress> findByUserIdAndFlashcardCourseId(Long userId, Long courseId);
    long countByUserIdAndFlashcardCourseIdAndIsFlippedTrue(Long userId, Long courseId);
}
