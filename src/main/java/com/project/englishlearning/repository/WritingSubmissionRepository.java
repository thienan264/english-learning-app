package com.project.englishlearning.repository;

import com.project.englishlearning.entity.WritingSubmission;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WritingSubmissionRepository extends JpaRepository<WritingSubmission, Long> {
    List<WritingSubmission> findByUserIdOrderBySubmittedAtDesc(Long userId);

}