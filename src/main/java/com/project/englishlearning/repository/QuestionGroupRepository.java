package com.project.englishlearning.repository;

import com.project.englishlearning.entity.QuestionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, Long> {
    List<QuestionGroup> findByLessonIdOrderByOrderIndexAsc(Long lessonId);
}
