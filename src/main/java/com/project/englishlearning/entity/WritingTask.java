package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "writing_tasks")
public class WritingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_type", nullable = false, length = 20)
    private String taskType; // "TASK_1" or "TASK_2"

    @Column(nullable = false, length = 300)
    private String title; // e.g. "Bar chart - Global energy consumption"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String instruction; // The full prompt text

    @Column(name = "min_words")
    private Integer minWords = 150; // Task 1 = 150, Task 2 = 250

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes = 20; // Task 1 = 20, Task 2 = 40

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "writingTask", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WritingTaskImage> images = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public Integer getMinWords() { return minWords; }
    public void setMinWords(Integer minWords) { this.minWords = minWords; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(Integer timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<WritingTaskImage> getImages() { return images; }
    public void setImages(List<WritingTaskImage> images) { this.images = images; }
}
