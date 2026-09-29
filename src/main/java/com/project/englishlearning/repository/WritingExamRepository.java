package com.project.englishlearning.repository;

import com.project.englishlearning.entity.WritingExam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WritingExamRepository extends JpaRepository<WritingExam, Long> {
    Optional<WritingExam> findByLessonId(Long lessonId);
    List<WritingExam> findAllByOrderByCreatedAtDesc();
}
