package com.project.englishlearning.repository;

import com.project.englishlearning.entity.FlashcardTestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FlashcardTestResultRepository extends JpaRepository<FlashcardTestResult, Long> {
    List<FlashcardTestResult> findByUserIdOrderByCompletedAtDesc(Long userId);
    void deleteByCourseId(Long courseId);
}
