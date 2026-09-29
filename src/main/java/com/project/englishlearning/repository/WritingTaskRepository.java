package com.project.englishlearning.repository;

import com.project.englishlearning.entity.WritingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WritingTaskRepository extends JpaRepository<WritingTask, Long> {
    List<WritingTask> findByTaskTypeOrderByCreatedAtDesc(String taskType);
}
