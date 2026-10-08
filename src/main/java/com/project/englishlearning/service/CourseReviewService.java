package com.project.englishlearning.service;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
public class CourseReviewService {
    private final CourseReviewRepository reviews;
    private final CourseOrderRepository orders;
    public record Stats(long purchases, double rating, long reviews) {}
    public CourseReviewService(CourseReviewRepository reviews, CourseOrderRepository orders) {
        this.reviews = reviews; this.orders = orders;
    }
    public Map<Long, Stats> statistics() {
        Map<Long, Stats> result = new HashMap<>();
        for (Object[] row : orders.countPurchasesByCourse()) result.put(((Number)row[0]).longValue(), new Stats(((Number)row[1]).longValue(), 0, 0));
        for (Object[] row : reviews.ratingStats()) {
            Long id = ((Number)row[0]).longValue();
            result.put(id, new Stats(result.getOrDefault(id, new Stats(0,0,0)).purchases(), ((Number)row[1]).doubleValue(), ((Number)row[2]).longValue()));
        }
        return result;
    }
    public boolean hasPurchased(Long userId, Long courseId) {
        return userId != null && orders.existsByUserIdAndCourseIdAndStatus(userId, courseId, "PAID");
    }
    public boolean canReview(User user, Course course) {
        return user != null && Boolean.FALSE.equals(course.getIsFree()) &&
            hasPurchased(user.getId(), course.getId());
    }
    @Transactional
    public void submit(User user, Course course, int rating, String comment) {
        if (!canReview(user, course)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Chỉ học viên đã mua khóa học mới được đánh giá.");
        if (rating < 0 || rating > 5 || (comment != null && comment.length() > 1500))
            throw new IllegalArgumentException("Chọn từ 0 đến 5 sao; nhận xét tối đa 1500 ký tự.");
        CourseReview review = reviews.findByCourseIdAndUserId(course.getId(), user.getId()).orElseGet(CourseReview::new);
        review.setCourse(course); review.setUser(user); review.setRating(rating);
        review.setComment(comment == null ? "" : comment.strip());
        review.setUpdatedAt(java.time.LocalDateTime.now());
        reviews.saveAndFlush(review);
    }
}
