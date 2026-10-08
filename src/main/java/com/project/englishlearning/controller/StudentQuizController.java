package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.entity.Question;
import com.project.englishlearning.entity.Answer;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.LessonRepository;
import com.project.englishlearning.repository.QuestionRepository;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.service.StudentLearningService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/lessons/{lessonId}")
public class StudentQuizController {

    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final StudentLearningService learningService;
    private final com.project.englishlearning.repository.TestResultRepository testResultRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public StudentQuizController(LessonRepository lessonRepository, QuestionRepository questionRepository,
                                 UserRepository userRepository, StudentLearningService learningService,
                                 com.project.englishlearning.repository.TestResultRepository testResultRepository) {
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.learningService = learningService;
        this.testResultRepository = testResultRepository;
    }

    @GetMapping("/take")
    public String takeQuiz(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        List<Question> questions = questionRepository.findByLessonId(lessonId);

        model.addAttribute("lesson", lesson);
        model.addAttribute("questions", questions);
        return "student/take-quiz";
    }

    @PostMapping("/submit-quiz")
    public String submitQuiz(@PathVariable Long lessonId,
                             jakarta.servlet.http.HttpServletRequest request,
                             Authentication authentication,
                             RedirectAttributes ra) {
        
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        List<Question> questions = questionRepository.findByLessonId(lessonId);

        int totalQuestions = questions.size();
        if (totalQuestions == 0) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Bài chưa có câu hỏi.");
        int correctAnswers = 0;

        com.fasterxml.jackson.databind.node.ArrayNode detailsArray = objectMapper.createArrayNode();

        for (Question q : questions) {
            String submittedValue = request.getParameter("question_" + q.getId());
            if (submittedValue == null) submittedValue = "";
            submittedValue = submittedValue.trim();

            boolean isCorrect = false;
            String correctAnswerText = "";
            String type = q.getQuestionType();

            if (type == null || "MULTIPLE_CHOICE".equals(type) || "MULTIPLE_CHOICE_SINGLE".equals(type)) {
                for (Answer a : q.getAnswers()) {
                    if (a.getIsCorrect()) {
                        correctAnswerText = a.getAnswerText();
                        if (a.getId().toString().equals(submittedValue)) {
                            isCorrect = true;
                        }
                    }
                }
            } else if ("TRUE_FALSE_NOT_GIVEN".equals(type) || "FILL_IN_THE_BLANK".equals(type)) {
                correctAnswerText = q.getCorrectAnswer();
                if (!submittedValue.isEmpty() && submittedValue.equalsIgnoreCase(q.getCorrectAnswer())) {
                    isCorrect = true;
                }
            }

            if (isCorrect) correctAnswers++;

            if (type == null || "MULTIPLE_CHOICE".equals(type) || "MULTIPLE_CHOICE_SINGLE".equals(type)) {
                for (Answer answer : q.getAnswers()) {
                    if (String.valueOf(answer.getId()).equals(submittedValue)) {
                        submittedValue = (answer.getLabel() == null ? "" : answer.getLabel() + ". ") + answer.getAnswerText();
                        break;
                    }
                }
            }
            com.fasterxml.jackson.databind.node.ObjectNode detailNode = objectMapper.createObjectNode();
            detailNode.put("questionId", q.getId());
            detailNode.put("questionType", type == null ? "MULTIPLE_CHOICE" : type);
            detailNode.put("competencyTag", q.getCompetencyTag());
            detailNode.put("learningLevel", q.getLearningLevel() != null ? q.getLearningLevel() : lesson.getLearningLevel());
            detailNode.put("questionText", q.getQuestionText());
            detailNode.put("submittedValue", submittedValue);
            detailNode.put("correctAnswer", correctAnswerText);
            detailNode.put("isCorrect", isCorrect);
            detailNode.put("explanation", q.getExplanation());
            detailsArray.add(detailNode);
        }

        // Calculate score (out of 10)
        double score = totalQuestions > 0 ? ((double) correctAnswers / totalQuestions) * 10.0 : 0.0;
        double passScore = lesson.getPassScore() != null ? lesson.getPassScore() : 0.0;
        
        com.project.englishlearning.entity.TestResult result = new com.project.englishlearning.entity.TestResult();
        result.setUser(user);
        result.setLesson(lesson);
        result.setTotalQuestions(totalQuestions);
        result.setCorrectAnswers(correctAnswers);
        result.setScore(Math.round(score * 10.0) / 10.0);
        try {
            result.setDetailedResultJson(objectMapper.writeValueAsString(detailsArray));
        } catch (Exception e) {
            e.printStackTrace();
        }
        testResultRepository.save(result);

        learningService.recordTestResult(result);

        return "redirect:/lessons/" + lessonId + "/result/" + result.getId();
    }

    @GetMapping("/quiz-result")
    public String quizResult(@PathVariable Long lessonId, Model model) {
        model.addAttribute("lesson", lessonRepository.findById(lessonId).orElseThrow());
        return "student/quiz-result";
    }
}

