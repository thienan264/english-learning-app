package com.project.englishlearning.service;

import com.project.englishlearning.entity.Question;
import java.util.List;

public interface QuestionService {
    List<Question> getQuestionsByLessonId(Long lessonId);
    void createMultipleChoiceQuestion(Long lessonId, String questionText, String explanation, String[] answers, int correctIndex);
}