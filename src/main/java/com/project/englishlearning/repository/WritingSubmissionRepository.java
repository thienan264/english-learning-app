package com.project.englishlearning.repository;

import com.project.englishlearning.entity.WritingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from WritingSubmission s where s.id = :id")
    java.util.Optional<WritingSubmission> lockForReview(@org.springframework.data.repository.query.Param("id") Long id);
    List<WritingSubmission> findByGradingModeOrderBySubmittedAtDesc(String mode);
    List<WritingSubmission> findByUserIdAndLessonIdAndGradingModeOrderBySubmittedAtAsc(Long userId, Long lessonId, String mode);
    List<WritingSubmission> findByUserId(Long userId);
    List<WritingSubmission> findByLessonId(Long lessonId);
    void deleteByLessonId(Long lessonId);
    List<WritingSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<WritingSubmission> findByUserIdAndLessonIdOrderBySubmittedAtDesc(Long userId, Long lessonId);
}
