package com.project.englishlearning.controller;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/courses")
public class StudentCourseController {
    private final CourseRepository courses;
    private final UserRepository users;
    private final UserCourseEnrollmentRepository enrollments;
    private final ModuleRepository modules;
    private final CourseReviewRepository reviews;
    private final CourseReviewService reviewService;
    private final CourseAccessService access;
    public StudentCourseController(CourseRepository courses, UserRepository users,
            UserCourseEnrollmentRepository enrollments, ModuleRepository modules,
            CourseReviewRepository reviews, CourseReviewService reviewService, CourseAccessService access) {
        this.courses=courses;this.users=users;this.enrollments=enrollments;this.modules=modules;
        this.reviews=reviews;this.reviewService=reviewService;this.access=access;
    }
    private User currentUser(Authentication auth) {
        return auth == null ? null : users.findByUsername(auth.getName()).orElse(null);
    }
    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String courseDetails(@PathVariable Long id, Model model, Authentication auth) {
        Course course = courses.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        User user = currentUser(auth);
        var enrollment = user == null ? null : enrollments.findByUserIdAndCourseId(user.getId(), id).orElse(null);
        boolean enrolled = enrollment != null && !"REVOKED".equals(enrollment.getStatus());
        boolean expired = enrolled && enrollment.getExpiresAt() != null && !java.time.LocalDateTime.now().isBefore(enrollment.getExpiresAt());
        var outline = modules.findByCourseIdOrderByOrderIndexAsc(id);
        outline.forEach(m -> m.getLessons().sort(java.util.Comparator.comparing(Lesson::getOrderIndex, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()))));
        var now = java.time.LocalDateTime.now();
        boolean saleActive = course.getSalePrice() != null && (course.getSaleStartDate() == null || !now.isBefore(course.getSaleStartDate())) && (course.getSaleEndDate() == null || now.isBefore(course.getSaleEndDate()));
        model.addAttribute("course", course);model.addAttribute("modules", outline);
        model.addAttribute("totalLessons", outline.stream().mapToInt(m -> m.getLessons().size()).sum());
        model.addAttribute("isEnrolled", enrolled);model.addAttribute("isExpired", expired);
        model.addAttribute("canLearn", access.canLearn(user, course));
        model.addAttribute("enrollment", enrollment);
        model.addAttribute("daysRemaining", enrollment == null || enrollment.getExpiresAt() == null ? null : Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(now, enrollment.getExpiresAt())));
        model.addAttribute("saleActive", saleActive);
        model.addAttribute("effectivePrice", saleActive ? course.getSalePrice() : course.getPrice());
        model.addAttribute("stats", reviewService.statistics().getOrDefault(id, new CourseReviewService.Stats(0,0,0)));
        model.addAttribute("reviews", reviews.visibleReviews(id));
        model.addAttribute("canReview", reviewService.canReview(user, course));
        model.addAttribute("myReview", user == null ? null : reviews.findByCourseIdAndUserId(id, user.getId()).orElse(null));
        return "student/course-details";
    }
    @PostMapping("/{id}/reviews")
    public String review(@PathVariable Long id, @RequestParam int rating,
            @RequestParam(defaultValue="") String comment, Authentication auth, RedirectAttributes flash) {
        Course course = courses.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        try {
            reviewService.submit(currentUser(auth), course, rating, comment);
            flash.addFlashAttribute("reviewSuccess", "Đã lưu đánh giá của bạn.");
        } catch (IllegalArgumentException | org.springframework.dao.DataIntegrityViolationException ex) {
            flash.addFlashAttribute("reviewError", "Không lưu được đánh giá. Chọn 0–5 sao và viết tối đa 1500 ký tự, rồi thử lại.");
        }
        return "redirect:/courses/" + id + "#reviews";
    }
}
