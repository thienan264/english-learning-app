package com.project.englishlearning.repository;

import com.project.englishlearning.entity.CourseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;
import java.math.BigDecimal;

public interface CourseOrderRepository extends JpaRepository<CourseOrder, Long> {
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, String status);
    Optional<CourseOrder> findByOrderCode(String orderCode);
    List<CourseOrder> findByUserId(Long userId);
    List<CourseOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CourseOrder> findByUserIdAndCourseId(Long userId, Long courseId);

    @Query("SELECT SUM(o.amount) FROM CourseOrder o WHERE o.status = 'PAID'")
    BigDecimal calculateTotalRevenue();

    @Query("SELECT MONTH(o.createdAt), SUM(o.amount) FROM CourseOrder o WHERE o.status = 'PAID' AND YEAR(o.createdAt) = YEAR(CURRENT_DATE) GROUP BY MONTH(o.createdAt) ORDER BY MONTH(o.createdAt)")
    List<Object[]> getMonthlyRevenueCurrentYear();

    @Query("select sum(o.amount) from CourseOrder o where o.status='PAID' and o.createdAt >= :start and o.createdAt < :end")
    BigDecimal revenueInPeriod(@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
                              @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

    @Query("SELECT o.course.title, COUNT(o.id) as cnt, o.course.id, o.course.price FROM CourseOrder o WHERE o.status = 'PAID' GROUP BY o.course.id, o.course.title, o.course.price ORDER BY cnt DESC LIMIT 5")
    List<Object[]> getTopSellingCourses();
    @Query("select month(o.createdAt), count(o.id) from CourseOrder o where o.status='PAID' and year(o.createdAt)=year(current_date) group by month(o.createdAt)")
    List<Object[]> monthlyPurchases();

    @Query("SELECT o.course.id, COUNT(o.id) FROM CourseOrder o WHERE o.status = 'PAID' GROUP BY o.course.id")
    List<Object[]> countPurchasesByCourse();

    List<CourseOrder> findAllByOrderByCreatedAtDesc();
}
