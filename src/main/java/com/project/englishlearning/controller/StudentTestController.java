package com.project.englishlearning.controller;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.IeltsScoringService;
import com.project.englishlearning.service.QuestionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/lessons")
public class StudentTestController {

    private final LessonRepository lessonRepository;
    private final QuestionService questionService;
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final IeltsScoringService ieltsScoringService;

    public StudentTestController(LessonRepository lessonRepository, QuestionService questionService,
            AnswerRepository answerRepository, UserRepository userRepository,
            TestResultRepository testResultRepository, IeltsScoringService ieltsScoringService) {
        this.lessonRepository = lessonRepository;
        this.questionService = questionService;
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.testResultRepository = testResultRepository;
        this.ieltsScoringService = ieltsScoringService; // Nối Service mới
    }

    @GetMapping("/{lessonId}")
    public String takeTest(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null)
            return "redirect:/";

        model.addAttribute("lesson", lesson);
        model.addAttribute("questions", questionService.getQuestionsByLessonId(lessonId));
        return "student/take-test";
    }

    @PostMapping("/{lessonId}/submit")
    public String submitTest(@PathVariable Long lessonId,
            @RequestParam Map<String, String> allParams,
            Authentication authentication,
            Model model) {

        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow();
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();

        List<Question> questions = questionService.getQuestionsByLessonId(lessonId);
        int totalQuestions = questions.size();
        int correctAnswers = 0;

        for (Question q : questions) {
            String questionKey = "question_" + q.getId();
            if (allParams.containsKey(questionKey)) {
                Long selectedAnswerId = Long.parseLong(allParams.get(questionKey));
                Answer selectedAnswer = answerRepository.findById(selectedAnswerId).orElse(null);

                if (selectedAnswer != null && selectedAnswer.getIsCorrect()) {
                    correctAnswers++;
                }
            }
        }

        double bandScore = ieltsScoringService.calculateBandScore(correctAnswers, totalQuestions);
        TestResult result = new TestResult();
        result.setUser(user);
        result.setLesson(lesson);
        result.setTotalQuestions(totalQuestions);
        result.setCorrectAnswers(correctAnswers);
        result.setScore(bandScore);
        testResultRepository.save(result);

        model.addAttribute("result", result);
        model.addAttribute("lesson", lesson);
        return "student/test-result";
    }
}