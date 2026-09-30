package com.project.englishlearning.repository;

import com.project.englishlearning.entity.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    List<TestResult> findByUserId(Long userId);
    List<TestResult> findByLessonId(Long lessonId);
    void deleteByLessonId(Long lessonId);
    List<TestResult> findByUserIdOrderByCompletedAtDesc(Long userId);
    List<TestResult> findByUserIdAndLessonIdOrderByCompletedAtDesc(Long userId, Long lessonId);
}
