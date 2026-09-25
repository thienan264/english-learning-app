package com.project.englishlearning.repository;

import com.project.englishlearning.entity.WritingSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {
}