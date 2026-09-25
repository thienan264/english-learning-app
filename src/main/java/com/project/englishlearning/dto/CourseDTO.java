package com.project.englishlearning.dto;

public class CourseDTO {
    
    private Long id;
    private String title;
    private String description;
    private String level;

    // Constructor rỗng
    public CourseDTO() {
    }

    // Constructor đầy đủ
    public CourseDTO(Long id, String title, String description, String level) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.level = level;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}