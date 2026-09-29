package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "writing_exams")
public class WritingExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false, length = 300)
    private String title; // e.g. "IELTS Academic Writing Test - Vol 1"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task1_id")
    private WritingTask task1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task2_id")
    private WritingTask task2;

    @Column(name = "total_time_minutes")
    private Integer totalTimeMinutes = 60;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public WritingTask getTask1() { return task1; }
    public void setTask1(WritingTask task1) { this.task1 = task1; }

    public WritingTask getTask2() { return task2; }
    public void setTask2(WritingTask task2) { this.task2 = task2; }

    public Integer getTotalTimeMinutes() { return totalTimeMinutes; }
    public void setTotalTimeMinutes(Integer totalTimeMinutes) { this.totalTimeMinutes = totalTimeMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
