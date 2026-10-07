package com.project.englishlearning.repository;

import com.project.englishlearning.entity.AdvisoryRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdvisoryRequestRepository extends JpaRepository<AdvisoryRequest, Long> {
    List<AdvisoryRequest> findAllByOrderByCreatedAtDesc();
}
