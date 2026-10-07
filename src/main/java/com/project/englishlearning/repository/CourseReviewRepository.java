package com.project.englishlearning.repository;
import com.project.englishlearning.entity.CourseReview;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {
    Optional<CourseReview> findByCourseIdAndUserId(Long courseId, Long userId);
    @Query("select r from CourseReview r join fetch r.user where r.course.id = :courseId and r.visible = true order by r.updatedAt desc")
    List<CourseReview> visibleReviews(@org.springframework.data.repository.query.Param("courseId") Long courseId);
    @Query("select r from CourseReview r join fetch r.user join fetch r.course order by r.updatedAt desc")
    List<CourseReview> adminReviews();
    @Query("select r from CourseReview r join fetch r.user join fetch r.course where r.course.id = :courseId order by r.updatedAt desc")
    List<CourseReview> adminReviewsByCourseId(@org.springframework.data.repository.query.Param("courseId") Long courseId);
    @Query("select r.course.id, avg(r.rating), count(r.id) from CourseReview r where r.visible = true group by r.course.id")
    List<Object[]> ratingStats();
    void deleteByCourseId(Long courseId);
}
