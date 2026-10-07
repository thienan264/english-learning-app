package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_reviews", uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "user_id"}))
public class CourseReview {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_id", nullable = false) private Course course;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false) private int rating;
    @Column(length = 1500) private String comment;
    @Column(nullable = false) private boolean visible = true;
    private LocalDateTime updatedAt = LocalDateTime.now();
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Course getCourse() { return course; }
    public void setCourse(Course v) { course = v; }
    public User getUser() { return user; }
    public void setUser(User v) { user = v; }
    public int getRating() { return rating; }
    public void setRating(int v) { rating = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { comment = v; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean v) { visible = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}
