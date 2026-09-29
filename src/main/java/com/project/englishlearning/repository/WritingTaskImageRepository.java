package com.project.englishlearning.repository;

import com.project.englishlearning.entity.WritingTaskImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingTaskImageRepository extends JpaRepository<WritingTaskImage, Long> {
    List<WritingTaskImage> findByWritingTaskIdOrderByOrderIndexAsc(Long writingTaskId);
}
