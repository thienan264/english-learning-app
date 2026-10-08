package com.project.englishlearning.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Safe projection shared by the learner page and future AI context. */
public record LearningSummary(String goal, List<Skill> skills, List<Course> courses,
                              int completedLessons, int excludedAttempts, int legacyQuestions,
                              LocalDateTime updatedAt) {
    public record Skill(String code, String label, String suggestedLevel, String levelLabel,
                        String basis, Long evidenceLessonId, Long evidenceResultId,
                        int questions, int correct, double accuracy, List<Competency> competencies) {}
    public record Competency(String code, String label, String level, String levelLabel, int questions, int correct,
                             double accuracy, String status, String statusLabel) {}
    public record Course(Long id, String title, String status, String statusLabel, double completion) {}
}
