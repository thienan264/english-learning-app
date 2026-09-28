package com.project.englishlearning.dto;

public class CourseDTO {
    
    private Long id;
    private String title;
    private String description;
    private String level;

    // ModelMapper bắt buộc phải có Constructor rỗng
    public CourseDTO() {
    }

    // --- BẠN HÃY TỰ ĐỘNG GENERATE GETTER & SETTER CHO 4 TRƯỜNG NÀY ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}