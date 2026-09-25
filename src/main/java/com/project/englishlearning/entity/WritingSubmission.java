package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "writing_submissions")
public class WritingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "submission_text", columnDefinition = "TEXT", nullable = false)
    private String submissionText;

    @Column(name = "band_score")
    private Double bandScore;

    @Column(columnDefinition = "TEXT")
    private String feedback; // Lời nhận xét chi tiết từ AI

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "suggested_essay", columnDefinition = "TEXT")
    private String suggestedEssay; // Bài viết gợi ý nâng cấp 1 band điểm
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public void setLesson(Lesson lesson) {
        this.lesson = lesson;
    }

    public String getSubmissionText() {
        return submissionText;
    }

    public void setSubmissionText(String submissionText) {
        this.submissionText = submissionText;
    }

    public Double getBandScore() {
        return bandScore;
    }

    public void setBandScore(Double bandScore) {
        this.bandScore = bandScore;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getSuggestedEssay() {
        return suggestedEssay;
    }

    public void setSuggestedEssay(String suggestedEssay) {
        this.suggestedEssay = suggestedEssay;
    }

    
}