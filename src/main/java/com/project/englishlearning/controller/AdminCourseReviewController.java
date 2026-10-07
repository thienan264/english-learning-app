package com.project.englishlearning.controller;
import com.project.englishlearning.repository.CourseReviewRepository;
import com.project.englishlearning.repository.CourseRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
@Controller
@RequestMapping("/admin/reviews")
public class AdminCourseReviewController {
    private final CourseReviewRepository reviews;
    private final CourseRepository courses;
    public AdminCourseReviewController(CourseReviewRepository reviews, CourseRepository courses) { this.reviews=reviews; this.courses=courses; }
    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly=true)
    public String list(@RequestParam(required=false) Long courseId, Model model) {
        if (courseId != null) model.addAttribute("selectedCourse", courses.findById(courseId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)));
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("courses", courses.findAll());
        model.addAttribute("reviews", courseId == null ? reviews.adminReviews() : reviews.adminReviewsByCourseId(courseId));
        return "admin/course-reviews";
    }
    @PostMapping("/{id}/visibility")
    public String visibility(@PathVariable Long id, @RequestParam boolean visible, @RequestParam(required=false) Long courseId) {
        var review = reviews.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        review.setVisible(visible);reviews.save(review);return "redirect:/admin/reviews" + (courseId != null && courseId.equals(review.getCourse().getId()) ? "?courseId=" + courseId : "");
    }
}
