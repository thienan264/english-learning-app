package com.project.englishlearning.controller;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.entity.Module;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.StudentLearningService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/learn")
public class StudentLearningController {

    private final StudentLearningService learningService;
    private final CourseRepository courseRepo;
    private final ModuleRepository moduleRepo;
    private final LessonRepository lessonRepo;
    private final LessonTheoryRepository theoryRepo;
    private final UserRepository userRepo;
    private final UserLessonProgressRepository progressRepo;
    private final UserCourseEnrollmentRepository enrollmentRepo;
    private final TestResultRepository testResultRepo;
    private final WritingSubmissionRepository writingSubmissionRepo;

    public StudentLearningController(StudentLearningService learningService,
                                     CourseRepository courseRepo, ModuleRepository moduleRepo,
                                     LessonRepository lessonRepo, LessonTheoryRepository theoryRepo,
                                     UserRepository userRepo, UserLessonProgressRepository progressRepo,
                                     UserCourseEnrollmentRepository enrollmentRepo,
                                     TestResultRepository testResultRepo,
                                     WritingSubmissionRepository writingSubmissionRepo) {
        this.learningService = learningService;
        this.courseRepo = courseRepo;
        this.moduleRepo = moduleRepo;
        this.lessonRepo = lessonRepo;
        this.theoryRepo = theoryRepo;
        this.userRepo = userRepo;
        this.progressRepo = progressRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.testResultRepo = testResultRepo;
        this.writingSubmissionRepo = writingSubmissionRepo;
    }

    @GetMapping("/course/{courseId}")
    public String enterCourse(@PathVariable Long courseId, 
                              @RequestParam(required = false) Long lessonId, 
                              Authentication auth, Model model) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        
        // Ensure user is enrolled & progress initialized
        learningService.initializeCourseProgress(user, courseId);
        
        Course course = courseRepo.findById(courseId).orElseThrow();
        List<Module> modules = moduleRepo.findByCourseIdOrderByOrderIndexAsc(courseId);
        
        // Sort lessons inside modules
        for(Module m : modules) {
            m.getLessons().sort((l1, l2) -> Integer.compare(
                l1.getOrderIndex() != null ? l1.getOrderIndex() : 0, 
                l2.getOrderIndex() != null ? l2.getOrderIndex() : 0));
        }

        // Fetch User Progress for all lessons in this course
        List<UserLessonProgress> allProgress = progressRepo.findByUserId(user.getId());
        Map<Long, UserLessonProgress> progressMap = new HashMap<>();
        for (UserLessonProgress p : allProgress) {
            progressMap.put(p.getLesson().getId(), p);
        }

        // Determine current lesson to display
        Lesson activeLesson = null;
        if (lessonId != null) {
            activeLesson = lessonRepo.findById(lessonId).orElse(null);
            // Check if locked
            UserLessonProgress p = progressMap.get(lessonId);
            if (activeLesson == null || activeLesson.getModule() == null || !courseId.equals(activeLesson.getModule().getCourse().getId()) || p == null || "LOCKED".equals(p.getStatus())) {
                activeLesson = null; // Deny access
            }
        }
        
        // If no lesson specified or denied, find the first UNLOCKED or IN_PROGRESS lesson
        if (activeLesson == null) {
            outer: for (Module m : modules) {
                for (Lesson l : m.getLessons()) {
                    UserLessonProgress p = progressMap.get(l.getId());
                    if (p != null && ("UNLOCKED".equals(p.getStatus()) || "IN_PROGRESS".equals(p.getStatus()))) {
                        activeLesson = l;
                        break outer;
                    }
                }
            }
            // If all completed, just show the first lesson
            if (activeLesson == null && !modules.isEmpty() && !modules.get(0).getLessons().isEmpty()) {
                activeLesson = modules.get(0).getLessons().get(0);
            }
        }

        // Load specific content based on lesson type
        if (activeLesson != null) {
            if ("THEORY".equals(activeLesson.getLessonType())) {
                LessonTheory theory = theoryRepo.findByLessonId(activeLesson.getId()).orElse(null);
                if (theory != null) {
                    String vUrl = theory.getVideoUrl();
                    if (vUrl != null) {
                        if (vUrl.contains("youtube.com/watch?v=")) {
                            vUrl = vUrl.replace("watch?v=", "embed/");
                            if (vUrl.contains("&")) vUrl = vUrl.substring(0, vUrl.indexOf("&"));
                        } else if (vUrl.contains("youtu.be/")) {
                            vUrl = vUrl.replace("youtu.be/", "youtube.com/embed/");
                            if (vUrl.contains("?")) vUrl = vUrl.substring(0, vUrl.indexOf("?"));
                        }
                        model.addAttribute("formattedVideoUrl", vUrl);
                    }
                }
                model.addAttribute("theoryContent", theory);
            }
            
            // Mark as IN_PROGRESS if it was UNLOCKED
            UserLessonProgress p = progressMap.get(activeLesson.getId());
            if (p != null && "UNLOCKED".equals(p.getStatus())) {
                p.setStatus("IN_PROGRESS");
                p.setLastAccessedAt(LocalDateTime.now());
                progressRepo.save(p);
                progressMap.put(activeLesson.getId(), p);
            }
        }

        UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(user.getId(), courseId).orElse(null);

        boolean isExpired = false;
        long daysUntilExpiration = -1;
        if (enrollment != null && enrollment.getExpiresAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(enrollment.getExpiresAt())) {
                isExpired = true;
                daysUntilExpiration = 0;
            } else {
                daysUntilExpiration = java.time.temporal.ChronoUnit.DAYS.between(now, enrollment.getExpiresAt());
            }
        }

        if (activeLesson != null && ("QUIZ".equals(activeLesson.getLessonType()) || "MOCK_TEST".equals(activeLesson.getLessonType()))) {
            List<TestResult> testHistory = testResultRepo.findByUserIdAndLessonIdOrderByCompletedAtDesc(user.getId(), activeLesson.getId());
            // Reconcile attempts submitted before test completion was connected to progress.
            for (TestResult attempt : testHistory) learningService.recordTestResult(attempt);
            progressMap.clear();
            for (UserLessonProgress progress : progressRepo.findByUserId(user.getId())) {
                progressMap.put(progress.getLesson().getId(), progress);
            }
            enrollment = enrollmentRepo.findByUserIdAndCourseId(user.getId(), courseId).orElse(enrollment);
            model.addAttribute("testHistory", testHistory);
            
            if ("MOCK_TEST".equals(activeLesson.getLessonType())) {
                List<WritingSubmission> writingHistory = writingSubmissionRepo.findByUserIdAndLessonIdOrderBySubmittedAtDesc(user.getId(), activeLesson.getId());
                model.addAttribute("writingHistory", writingHistory);
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("modules", modules);
        model.addAttribute("progressMap", progressMap);
        model.addAttribute("activeLesson", activeLesson);
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("isExpired", isExpired);
        model.addAttribute("daysUntilExpiration", daysUntilExpiration);

        return "student/learning-dashboard";
    }

    @PostMapping("/lesson/{lessonId}/complete")
    public String completeLesson(@PathVariable Long lessonId, Authentication auth, RedirectAttributes redirectAttributes) {
        User user = userRepo.findByUsername(auth.getName()).orElseThrow();
        Lesson lesson = lessonRepo.findById(lessonId).orElseThrow();
        
        UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(user.getId(), lesson.getModule().getCourse().getId()).orElse(null);
        if (enrollment != null && enrollment.getExpiresAt() != null && LocalDateTime.now().isAfter(enrollment.getExpiresAt())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Khóa học đã hết hạn. Bạn không thể hoàn thành bài học.");
            return "redirect:/learn/course/" + lesson.getModule().getCourse().getId();
        }
        
        learningService.markLessonCompleted(user, lessonId);
        
        return "redirect:/learn/course/" + lesson.getModule().getCourse().getId();
    }
}
