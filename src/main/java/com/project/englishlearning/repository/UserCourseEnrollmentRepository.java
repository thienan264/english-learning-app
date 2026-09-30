package com.project.englishlearning.repository;

import com.project.englishlearning.entity.UserCourseEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCourseEnrollmentRepository extends JpaRepository<UserCourseEnrollment, Long> {
    Optional<UserCourseEnrollment> findByUserIdAndCourseId(Long userId, Long courseId);
    java.util.List<UserCourseEnrollment> findByUserId(Long userId);
}
