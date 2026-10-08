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
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager constraintLocks;
    private void lockLearner(Long id) {
        if (constraintLocks != null) constraintLocks.createNativeQuery("select id from users where id=:id for update").setParameter("id",id).getSingleResult();
    }

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
        lockLearner(user.getId());
        boolean isNewEnrollment = false;
        UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(user.getId(), courseId).orElse(null);
        if (enrollment != null && "REVOKED".equals(enrollment.getStatus())) {
            throw new RuntimeException("Khóa học đã bị thu hồi quyền truy cập!");
        }
        if (enrollment == null) {
            Course course = courseRepo.findById(courseId).orElseThrow();
            if (course.getIsFree() != null && !course.getIsFree()) {
                throw new RuntimeException("Bạn chưa mua khóa học này!");
            }
            enrollment = new UserCourseEnrollment();
            enrollment.setUser(user);
            enrollment.setCourse(course);
            enrollment.setStatus("IN_PROGRESS");
            enrollmentRepo.save(enrollment);
            isNewEnrollment = true;
        }

        List<Module> modules = moduleRepo.findByCourseIdOrderByOrderIndexAsc(courseId);
        boolean isFirst = true;
        boolean previousCompleted = false;
        boolean previousWasMockTestUnlocked = false;

        for (Module module : modules) {
            module.getLessons().sort((l1, l2) -> Integer.compare(
                    l1.getOrderIndex() != null ? l1.getOrderIndex() : 0, 
                    l2.getOrderIndex() != null ? l2.getOrderIndex() : 0));
            
            for (Lesson lesson : module.getLessons()) {
                boolean isMock = "MOCK_TEST".equals(lesson.getLessonType());
                boolean shouldUnlock = isFirst || previousCompleted || (isMock && previousWasMockTestUnlocked);

                Optional<UserLessonProgress> existing = progressRepo.findByUserIdAndLessonId(user.getId(), lesson.getId());
                if (existing.isEmpty()) {
                    UserLessonProgress progress = new UserLessonProgress();
                    progress.setUser(user);
                    progress.setLesson(lesson);
                    if (shouldUnlock) {
                        progress.setStatus("UNLOCKED");
                    } else {
                        progress.setStatus("LOCKED");
                    }
                    progressRepo.save(progress);
                    
                    isFirst = false;
                    previousCompleted = false;
                    previousWasMockTestUnlocked = (shouldUnlock && isMock);
                } else {
                    isFirst = false;
                    UserLessonProgress prog = existing.get();
                    if ("COMPLETED".equals(prog.getStatus())) {
                        previousCompleted = true;
                        previousWasMockTestUnlocked = false;
                    } else {
                        // Self-healing or cascading unlock for Mock Tests
                        if ("LOCKED".equals(prog.getStatus()) && shouldUnlock) {
                            prog.setStatus("UNLOCKED");
                            progressRepo.save(prog);
                        }
                        
                        previousCompleted = false;
                        previousWasMockTestUnlocked = (("UNLOCKED".equals(prog.getStatus()) || "IN_PROGRESS".equals(prog.getStatus())) && isMock);
                    }
                }
            }
        }
    }

    @Transactional
    public void recordTestResult(TestResult result) {
        Lesson lesson = result.getLesson();
        if (lesson.getModule() == null || result.getTotalQuestions() == null || result.getTotalQuestions() <= 0) return;
        User user = result.getUser();
        initializeCourseProgress(user, lesson.getModule().getCourse().getId());
        UserLessonProgress progress = progressRepo.findByUserIdAndLessonId(user.getId(), lesson.getId()).orElseThrow();
        double score = result.getPracticeScore();
        if (progress.getHighestScore() == null || progress.getHighestScore() < score) {
            progress.setHighestScore(score);
            progressRepo.save(progress);
        }
        double threshold = lesson.getPassScore() == null ? 0.0 : lesson.getPassScore();
        if ("FINAL".equals(lesson.getAssessmentRole()) && threshold <= 0) threshold = 8.0;
        // Completing practice records participation; it does not certify mastery.
        if ("PRACTICE".equals(lesson.getAssessmentRole()) || score >= threshold) {
            markLessonCompleted(user, lesson.getId());
        }
    }

    @Transactional
    public void markLessonCompleted(User user, Long lessonId) {
        lockLearner(user.getId());
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
        boolean unlockingMockTests = false;

        for (Module module : modules) {
            module.getLessons().sort((l1, l2) -> Integer.compare(
                    l1.getOrderIndex() != null ? l1.getOrderIndex() : 0, 
                    l2.getOrderIndex() != null ? l2.getOrderIndex() : 0));
                    
            for (Lesson lesson : module.getLessons()) {
                if (foundCurrent) {
                    boolean isMock = "MOCK_TEST".equals(lesson.getLessonType());
                    if (unlockingMockTests && !isMock) {
                        return; // Stop cascading if we hit a non-mock test
                    }

                    UserLessonProgress nextProgress = progressRepo.findByUserIdAndLessonId(user.getId(), lesson.getId()).orElse(null);
                    if (nextProgress != null && "LOCKED".equals(nextProgress.getStatus())) {
                        nextProgress.setStatus("UNLOCKED");
                        progressRepo.save(nextProgress);
                    }
                    
                    if (isMock) {
                        unlockingMockTests = true;
                        continue; // Keep unlocking the next ones if they are also MOCK_TESTs
                    } else {
                        return; // Only unlock ONE next lesson (or a contiguous block of MOCK_TESTs)
                    }
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
