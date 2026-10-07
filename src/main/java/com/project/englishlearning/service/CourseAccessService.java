package com.project.englishlearning.service;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.UserCourseEnrollmentRepository;
import org.springframework.stereotype.Service;
@Service
public class CourseAccessService {
    private final UserCourseEnrollmentRepository enrollments;
    public CourseAccessService(UserCourseEnrollmentRepository enrollments) { this.enrollments = enrollments; }
    public boolean canLearn(User user, Course course) {
        if (user == null) return false;
        var enrollment = enrollments.findByUserIdAndCourseId(user.getId(), course.getId()).orElse(null);
        if (enrollment != null && "REVOKED".equals(enrollment.getStatus())) return false;
        if (!Boolean.FALSE.equals(course.getIsFree())) return true;
        return enrollment != null && (enrollment.getExpiresAt() == null || java.time.LocalDateTime.now().isBefore(enrollment.getExpiresAt()));
    }
}
