package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.service.LessonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/courses")
public class StudentCourseController {

    private final CourseRepository courseRepository;
    private final LessonService lessonService;
    private final com.project.englishlearning.repository.UserRepository userRepository;
    private final com.project.englishlearning.repository.UserCourseEnrollmentRepository enrollmentRepository;

    public StudentCourseController(CourseRepository courseRepository, LessonService lessonService,
                                   com.project.englishlearning.repository.UserRepository userRepository,
                                   com.project.englishlearning.repository.UserCourseEnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.lessonService = lessonService;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/{id}")
    public String courseDetails(@PathVariable Long id, Model model, org.springframework.security.core.Authentication auth) {
        Course course = courseRepository.findById(id).orElseThrow();
        boolean isEnrolled = false;
        boolean isExpired = false;
        if (auth != null) {
            com.project.englishlearning.entity.User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                com.project.englishlearning.entity.UserCourseEnrollment enrollment = enrollmentRepository.findByUserIdAndCourseId(user.getId(), id).orElse(null);
                isEnrolled = enrollment != null && !"REVOKED".equals(enrollment.getStatus());
                if (isEnrolled && enrollment.getExpiresAt() != null && java.time.LocalDateTime.now().isAfter(enrollment.getExpiresAt())) {
                    isExpired = true;
                }
            }
        }
        
        model.addAttribute("course", course);
        model.addAttribute("isEnrolled", isEnrolled);
        model.addAttribute("isExpired", isExpired);
        model.addAttribute("lessons", lessonService.getLessonsByCourseId(id));
        return "student/course-details";
    }
}