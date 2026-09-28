package com.project.englishlearning.repository;

import com.project.englishlearning.entity.WritingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {
    List<WritingSubmission> findByUserId(Long userId);
    List<WritingSubmission> findByLessonId(Long lessonId);
    void deleteByLessonId(Long lessonId);
    List<WritingSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);
}
