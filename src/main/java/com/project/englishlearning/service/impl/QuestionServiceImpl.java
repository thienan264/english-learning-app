package com.project.englishlearning.service.impl;

import com.project.englishlearning.entity.Answer;
import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.entity.Question;
import com.project.englishlearning.repository.LessonRepository;
import com.project.englishlearning.repository.QuestionRepository;
import com.project.englishlearning.service.QuestionService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final LessonRepository lessonRepository;

    public QuestionServiceImpl(QuestionRepository questionRepository, LessonRepository lessonRepository) {
        this.questionRepository = questionRepository;
        this.lessonRepository = lessonRepository;
    }

    @Override
    public List<Question> getQuestionsByLessonId(Long lessonId) {
        return questionRepository.findByLessonId(lessonId);
    }

    @Override
    public void createMultipleChoiceQuestion(Long lessonId, String questionText, String explanation, String[] answers,
            int correctIndex) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();

        Question q = new Question();
        q.setLesson(lesson);
        q.setQuestionText(questionText);
        q.setQuestionType("MULTIPLE_CHOICE");
        q.setExplanation(explanation);

        for (int i = 0; i < answers.length; i++) {
            Answer a = new Answer();
            a.setAnswerText(answers[i]);
            a.setIsCorrect(i == correctIndex);
            a.setQuestion(q);
            q.getAnswers().add(a);
        }

        questionRepository.save(q);
    }
}