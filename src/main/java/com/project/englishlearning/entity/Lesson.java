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
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "linked_exam_id")
    private Long linkedExamId;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    private Module module;

    @Column(name = "lesson_type", length = 50)
    private String lessonType; // THEORY, QUIZ, MOCK_TEST

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "is_required")
    private Boolean isRequired = true;

    @Column(name = "pass_score")
    private Double passScore = 0.0;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<UserLessonProgress> progressList = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<LessonTheory> theories = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ExamPassage> examPassages = new ArrayList<>();

    @OneToOne(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private WritingExam writingExam;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<QuestionGroup> questionGroups = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TestResult> testResults = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<WritingSubmission> writingSubmissions = new ArrayList<>();

    // Nullable for existing content: unknown metadata must not imply assessed ability.
    @Column(name = "learning_level", length = 20)
    private String learningLevel;

    @Column(name = "learning_objective", length = 500)
    private String learningObjective;

    @Column(name = "assessment_role", length = 20)
    private String assessmentRole;

    public String getLearningLevel() { return learningLevel; }
    public void setLearningLevel(String value) { learningLevel = value; }
    public String getLearningObjective() { return learningObjective; }
    public void setLearningObjective(String value) { learningObjective = value; }
    public String getAssessmentRole() { return assessmentRole; }
    public void setAssessmentRole(String value) { assessmentRole = value; }

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

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public Module getModule() {
        return module;
    }

    public void setModule(Module module) {
        this.module = module;
    }

    public String getLessonType() {
        return lessonType;
    }

    public void setLessonType(String lessonType) {
        this.lessonType = lessonType;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Double getPassScore() {
        return passScore;
    }

    public void setPassScore(Double passScore) {
        this.passScore = passScore;
    }

    public Long getLinkedExamId() {
        return linkedExamId;
    }

    public void setLinkedExamId(Long linkedExamId) {
        this.linkedExamId = linkedExamId;
    }
}