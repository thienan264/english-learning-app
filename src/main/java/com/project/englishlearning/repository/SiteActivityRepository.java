package com.project.englishlearning.repository;
import com.project.englishlearning.entity.SiteActivity;
import org.springframework.data.jpa.repository.*;
import java.util.List;
public interface SiteActivityRepository extends JpaRepository<SiteActivity,Long> {
    @Query("select month(a.recordedAt), sum(a.pageViews), sum(a.studySeconds) from SiteActivity a where year(a.recordedAt)=year(current_date) group by month(a.recordedAt)")
    List<Object[]> monthlyActivity();
    @Query("select min(a.recordedAt) from SiteActivity a")
    java.time.LocalDateTime trackingStartedAt();
}
