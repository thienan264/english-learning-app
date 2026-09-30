package com.project.englishlearning.service;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.entity.Module;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StudentLearningService {
    private final UserCourseEnrollmentRepository enrollmentRepo;
    private final UserLessonProgressRepository progressRepo;
    private final CourseRepository courseRepo;
    private final ModuleRepository moduleRepo;
    private final LessonRepository lessonRepo;

    public StudentLearningService(UserCourseEnrollmentRepository enrollmentRepo,
                                  UserLessonProgressRepository progressRepo,
                                  CourseRepository courseRepo,
                                  ModuleRepository moduleRepo,
                                  LessonRepository lessonRepo) {
        this.enrollmentRepo = enrollmentRepo;
        this.progressRepo = progressRepo;
        this.courseRepo = courseRepo;
        this.moduleRepo = moduleRepo;
        this.lessonRepo = lessonRepo;
    }

    @Transactional
    public void initializeCourseProgress(User user, Long courseId) {
        if (enrollmentRepo.findByUserIdAndCourseId(user.getId(), courseId).isPresent()) {
            return; // Already initialized
        }

        Course course = courseRepo.findById(courseId).orElseThrow();
        UserCourseEnrollment enrollment = new UserCourseEnrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus("IN_PROGRESS");
        enrollmentRepo.save(enrollment);

        List<Module> modules = moduleRepo.findByCourseIdOrderByOrderIndexAsc(courseId);
        boolean isFirst = true;

        for (Module module : modules) {
            module.getLessons().sort((l1, l2) -> Integer.compare(
                    l1.getOrderIndex() != null ? l1.getOrderIndex() : 0, 
                    l2.getOrderIndex() != null ? l2.getOrderIndex() : 0));
            
            for (Lesson lesson : module.getLessons()) {
                UserLessonProgress progress = new UserLessonProgress();
                progress.setUser(user);
                progress.setLesson(lesson);
                progress.setStatus(isFirst ? "UNLOCKED" : "LOCKED");
                progressRepo.save(progress);
                isFirst = false;
            }
        }
    }

    @Transactional
    public void markLessonCompleted(User user, Long lessonId) {
        UserLessonProgress progress = progressRepo.findByUserIdAndLessonId(user.getId(), lessonId).orElseThrow();
        if ("COMPLETED".equals(progress.getStatus())) return;

        progress.setStatus("COMPLETED");
        progress.setCompletedAt(LocalDateTime.now());
        progressRepo.save(progress);

        // Unlock next lesson
        unlockNextLesson(user, progress.getLesson().getModule().getCourse().getId(), lessonId);
        
        // Update Course Completion
        updateCourseCompletion(user, progress.getLesson().getModule().getCourse().getId());
    }

    private void unlockNextLesson(User user, Long courseId, Long currentLessonId) {
        List<Module> modules = moduleRepo.findByCourseIdOrderByOrderIndexAsc(courseId);
        boolean foundCurrent = false;

        for (Module module : modules) {
            module.getLessons().sort((l1, l2) -> Integer.compare(
                    l1.getOrderIndex() != null ? l1.getOrderIndex() : 0, 
                    l2.getOrderIndex() != null ? l2.getOrderIndex() : 0));
                    
            for (Lesson lesson : module.getLessons()) {
                if (foundCurrent) {
                    UserLessonProgress nextProgress = progressRepo.findByUserIdAndLessonId(user.getId(), lesson.getId()).orElse(null);
                    if (nextProgress != null && "LOCKED".equals(nextProgress.getStatus())) {
                        nextProgress.setStatus("UNLOCKED");
                        progressRepo.save(nextProgress);
                    }
                    return; // Only unlock ONE next lesson
                }
                if (lesson.getId().equals(currentLessonId)) {
                    foundCurrent = true;
                }
            }
        }
    }

    private void updateCourseCompletion(User user, Long courseId) {
        UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(user.getId(), courseId).orElseThrow();
        List<Module> modules = moduleRepo.findByCourseIdOrderByOrderIndexAsc(courseId);
        
        int totalLessons = 0;
        int completedLessons = 0;

        for (Module module : modules) {
            for (Lesson lesson : module.getLessons()) {
                totalLessons++;
                Optional<UserLessonProgress> prog = progressRepo.findByUserIdAndLessonId(user.getId(), lesson.getId());
                if (prog.isPresent() && "COMPLETED".equals(prog.get().getStatus())) {
                    completedLessons++;
                }
            }
        }

        if (totalLessons > 0) {
            double pct = (double) completedLessons / totalLessons * 100.0;
            enrollment.setCompletionPercentage(Math.round(pct * 10.0) / 10.0);
            if (completedLessons == totalLessons) {
                enrollment.setStatus("COMPLETED");
                enrollment.setCompletedAt(LocalDateTime.now());
            }
            enrollmentRepo.save(enrollment);
        }
    }
}
