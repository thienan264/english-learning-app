package com.project.englishlearning.repository;

import com.project.englishlearning.entity.ExamPassage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamPassageRepository extends JpaRepository<ExamPassage, Long> {
    List<ExamPassage> findByLessonIdOrderByOrderIndexAsc(Long lessonId);
}
