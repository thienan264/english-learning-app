package com.project.englishlearning.repository;

import com.project.englishlearning.entity.CourseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

public interface CourseOrderRepository extends JpaRepository<CourseOrder, Long> {
    Optional<CourseOrder> findByOrderCode(String orderCode);
    List<CourseOrder> findByUserId(Long userId);
    List<CourseOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CourseOrder> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT SUM(o.amount) FROM CourseOrder o WHERE o.status = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT MONTH(o.createdAt), SUM(o.amount) FROM CourseOrder o WHERE o.status = 'PAID' AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenueCurrentYear();

    @Query("SELECT o.course.title, COUNT(o.id) as cnt FROM CourseOrder o WHERE o.status = 'PAID' GROUP BY o.course.title ORDER BY cnt DESC LIMIT 5")
    List<Object[]> getTopSellingCourses();

    @Query("SELECT o.course.id, COUNT(o.id) FROM CourseOrder o WHERE o.status = 'PAID' GROUP BY o.course.id")
    List<Object[]> countPurchasesByCourse();

    List<CourseOrder> findAllByOrderByCreatedAtDesc();
}
