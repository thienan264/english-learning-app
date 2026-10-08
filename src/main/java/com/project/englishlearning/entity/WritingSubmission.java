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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writing_exam_id")
    private WritingExam writingExam;

    @Column(name = "task1_essay", columnDefinition = "TEXT")
    private String task1Essay;

    @Column(name = "task2_essay", columnDefinition = "TEXT")
    private String task2Essay;

    // Keep the old field for backward compatibility
    @Column(name = "submission_text", columnDefinition = "TEXT")
    private String submissionText;

    // Overall band score
    @Column(name = "band_score")
    private Double bandScore;

    // 4 criteria scores for Task 1
    @Column(name = "task1_ta_score")
    private Double task1TaScore; // Task Achievement

    @Column(name = "task1_cc_score")
    private Double task1CcScore; // Coherence & Cohesion

    @Column(name = "task1_lr_score")
    private Double task1LrScore; // Lexical Resource

    @Column(name = "task1_gra_score")
    private Double task1GraScore; // Grammatical Range & Accuracy

    @Column(name = "task1_overall")
    private Double task1Overall;

    // 4 criteria scores for Task 2
    @Column(name = "task2_tr_score")
    private Double task2TrScore; // Task Response

    @Column(name = "task2_cc_score")
    private Double task2CcScore;

    @Column(name = "task2_lr_score")
    private Double task2LrScore;

    @Column(name = "task2_gra_score")
    private Double task2GraScore;

    @Column(name = "task2_overall")
    private Double task2Overall;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "feedback_json", columnDefinition = "TEXT")
    private String feedbackJson; // Full JSON response from AI for detailed rendering

    @Column(name = "suggested_essay", columnDefinition = "TEXT")
    private String suggestedEssay;

    @Column(name = "status", length = 20)
    private String status = "PENDING"; // PENDING, EVALUATING, COMPLETED, ERROR

    @Column(name = "is_off_topic")
    private Boolean isOffTopic = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    @Column(columnDefinition = "TEXT")
    private String expertReviewJson;
    public String getExpertReviewJson() { return expertReviewJson; }
    public void setExpertReviewJson(String value) { expertReviewJson=value; }

    private String gradingMode;
    private Integer expertAttempt;
    @Column(columnDefinition = "TEXT")
    private String promptSnapshot;
    @Column(columnDefinition = "TEXT")
    private String expertCorrections;
    private String reviewedBy;

    // ==============================================================
    // GETTER & SETTER
    // ==============================================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public WritingExam getWritingExam() { return writingExam; }
    public void setWritingExam(WritingExam writingExam) { this.writingExam = writingExam; }

    public String getTask1Essay() { return task1Essay; }
    public void setTask1Essay(String task1Essay) { this.task1Essay = task1Essay; }

    public String getTask2Essay() { return task2Essay; }
    public void setTask2Essay(String task2Essay) { this.task2Essay = task2Essay; }

    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }

    public Double getBandScore() { return bandScore; }
    public void setBandScore(Double bandScore) { this.bandScore = bandScore; }

    public Double getTask1TaScore() { return task1TaScore; }
    public void setTask1TaScore(Double task1TaScore) { this.task1TaScore = task1TaScore; }

    public Double getTask1CcScore() { return task1CcScore; }
    public void setTask1CcScore(Double task1CcScore) { this.task1CcScore = task1CcScore; }

    public Double getTask1LrScore() { return task1LrScore; }
    public void setTask1LrScore(Double task1LrScore) { this.task1LrScore = task1LrScore; }

    public Double getTask1GraScore() { return task1GraScore; }
    public void setTask1GraScore(Double task1GraScore) { this.task1GraScore = task1GraScore; }

    public Double getTask1Overall() { return task1Overall; }
    public void setTask1Overall(Double task1Overall) { this.task1Overall = task1Overall; }

    public Double getTask2TrScore() { return task2TrScore; }
    public void setTask2TrScore(Double task2TrScore) { this.task2TrScore = task2TrScore; }

    public Double getTask2CcScore() { return task2CcScore; }
    public void setTask2CcScore(Double task2CcScore) { this.task2CcScore = task2CcScore; }

    public Double getTask2LrScore() { return task2LrScore; }
    public void setTask2LrScore(Double task2LrScore) { this.task2LrScore = task2LrScore; }

    public Double getTask2GraScore() { return task2GraScore; }
    public void setTask2GraScore(Double task2GraScore) { this.task2GraScore = task2GraScore; }

    public Double getTask2Overall() { return task2Overall; }
    public void setTask2Overall(Double task2Overall) { this.task2Overall = task2Overall; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getFeedbackJson() { return feedbackJson; }
    public void setFeedbackJson(String feedbackJson) { this.feedbackJson = feedbackJson; }

    public String getSuggestedEssay() { return suggestedEssay; }
    public void setSuggestedEssay(String suggestedEssay) { this.suggestedEssay = suggestedEssay; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsOffTopic() { return isOffTopic; }
    public void setIsOffTopic(Boolean isOffTopic) { this.isOffTopic = isOffTopic; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(LocalDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public String getGradingMode() { return gradingMode; }
    public void setGradingMode(String value) { gradingMode = value; }
    public Integer getExpertAttempt() { return expertAttempt; }
    public void setExpertAttempt(Integer value) { expertAttempt = value; }
    public String getPromptSnapshot() { return promptSnapshot; }
    public void setPromptSnapshot(String value) { promptSnapshot = value; }
    public String getExpertCorrections() { return expertCorrections; }
    public void setExpertCorrections(String value) { expertCorrections = value; }
    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String value) { reviewedBy = value; }
    private String expertText(String value) {
        // HTML normalizes CRLF/CR to LF; annotation offsets must use that same text.
        return value == null ? "" : value.replace("\r\n", "\n").replace('\r', '\n');
    }
    public String getExpertTask1Text() { return expertText(task1Essay); }
    public String getExpertTask2Text() {
        return writingExam == null && (task2Essay == null || task2Essay.isBlank()) && (task1Essay == null || task1Essay.isBlank())
            ? expertText(submissionText) : expertText(task2Essay);
    }
}
