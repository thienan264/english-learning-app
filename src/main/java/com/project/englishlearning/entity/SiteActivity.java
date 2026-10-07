package com.project.englishlearning.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(indexes=@Index(name="idx_activity_recorded",columnList="recordedAt"))
public class SiteActivity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private LocalDateTime recordedAt=LocalDateTime.now();
    private long pageViews;
    private long studySeconds;
    public SiteActivity() {}
    public SiteActivity(long pageViews,long studySeconds) {this.pageViews=pageViews;this.studySeconds=studySeconds;}
}
