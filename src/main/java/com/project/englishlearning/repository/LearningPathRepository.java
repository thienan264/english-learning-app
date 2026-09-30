package com.project.englishlearning.repository;

import com.project.englishlearning.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LearningPathRepository extends JpaRepository<LearningPath, Long> {
    List<LearningPath> findAllByOrderByOrderIndexAsc();
}
