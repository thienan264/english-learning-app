package com.project.englishlearning.dto;

public class CourseDTO {
    
    private Long id;
    private String title;
    private String description;
    private String level;
    
    // Progress fields
    private Boolean isEnrolled = false;
    private Double completionPercentage = 0.0;
    private Integer completedLessons = 0;
    private Integer totalLessons = 0;
    private Integer totalFlashcards = 0;
    private Integer completedFlashcards = 0;

    // ModelMapper bắt buộc phải có Constructor rỗng
    public CourseDTO() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public Boolean getIsEnrolled() { return isEnrolled; }
    public void setIsEnrolled(Boolean enrolled) { isEnrolled = enrolled; }
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    public Integer getCompletedLessons() { return completedLessons; }
    public void setCompletedLessons(Integer completedLessons) { this.completedLessons = completedLessons; }
    public Integer getTotalLessons() { return totalLessons; }
    public void setTotalLessons(Integer totalLessons) { this.totalLessons = totalLessons; }
    public Integer getTotalFlashcards() { return totalFlashcards; }
    public void setTotalFlashcards(Integer totalFlashcards) { this.totalFlashcards = totalFlashcards; }
    public Integer getCompletedFlashcards() { return completedFlashcards; }
    public void setCompletedFlashcards(Integer completedFlashcards) { this.completedFlashcards = completedFlashcards; }
}