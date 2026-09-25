package com.project.englishlearning.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "skill_type", nullable = false, length = 20)
    private String skillType; 

    @Column(columnDefinition = "TEXT")
    private String content; 

    @Column(name = "media_url")
    private String mediaUrl; 

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "order_index")
    private Integer orderIndex = 1;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions = new ArrayList<>();

    // ==============================================================
    // GETTER & SETTER
    // ==============================================================
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSkillType() {
        return skillType;
    }

    public void setSkillType(String skillType) {
        this.skillType = skillType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    // Getter và Setter cho Questions
    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }
}