package com.project.englishlearning.repository;

import com.project.englishlearning.entity.LessonTheory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LessonTheoryRepository extends JpaRepository<LessonTheory, Long> {
    Optional<LessonTheory> findByLessonId(Long lessonId);
}
