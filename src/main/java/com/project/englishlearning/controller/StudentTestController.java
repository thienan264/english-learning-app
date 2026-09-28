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
    private final ExamPassageRepository passageRepository;
    private final QuestionGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final IeltsScoringService ieltsScoringService;

    public StudentTestController(LessonRepository lessonRepository, ExamPassageRepository passageRepository,
            QuestionGroupRepository groupRepository, UserRepository userRepository,
            TestResultRepository testResultRepository, IeltsScoringService ieltsScoringService) {
        this.lessonRepository = lessonRepository;
        this.passageRepository = passageRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.testResultRepository = testResultRepository;
        this.ieltsScoringService = ieltsScoringService;
    }

    @GetMapping("/{lessonId}")
    public String takeTest(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) return "redirect:/";

        model.addAttribute("lesson", lesson);
        model.addAttribute("passages", passageRepository.findByLessonIdOrderByOrderIndexAsc(lessonId));
        model.addAttribute("questionGroups", groupRepository.findByLessonIdOrderByOrderIndexAsc(lessonId));
        return "student/take-test";
    }

    @PostMapping("/{lessonId}/submit")
    public String submitTest(@PathVariable Long lessonId,
            jakarta.servlet.http.HttpServletRequest request,
            Authentication authentication,
            Model model) {

        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow();
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();

        List<QuestionGroup> groups = groupRepository.findByLessonIdOrderByOrderIndexAsc(lessonId);
        int totalQuestions = 0;
        int correctAnswers = 0;

        for (QuestionGroup g : groups) {
            for (Question q : g.getQuestions()) {
                totalQuestions++;
                String questionKey = "question_" + q.getId();
                String[] submittedValues = request.getParameterValues(questionKey);
                
                if (submittedValues == null || submittedValues.length == 0 || submittedValues[0].trim().isEmpty()) {
                    continue;
                }

                String qType = q.getQuestionType() != null ? q.getQuestionType() : g.getQuestionType().name();
                boolean isCorrect = false;

                if ("MULTIPLE_CHOICE_SINGLE".equals(qType) || "TRUE_FALSE_NOT_GIVEN".equals(qType)) {
                    // For single choice, we expect the label (A, B, C, True, False)
                    String ans = submittedValues[0].trim();
                    if ("MULTIPLE_CHOICE_SINGLE".equals(qType)) {
                        for (Answer a : q.getAnswers()) {
                            if (a.getIsCorrect() && (a.getId().toString().equals(ans) || (a.getLabel() != null && a.getLabel().equalsIgnoreCase(ans)))) {
                                isCorrect = true;
                                break;
                            }
                        }
                    } else {
                        // T/F/NG
                        if (ans.equalsIgnoreCase(q.getCorrectAnswer())) {
                            isCorrect = true;
                        }
                    }
                } else if ("MULTIPLE_CHOICE_MULTI".equals(qType)) {
                    // Expecting multiple correct answers. All must match perfectly.
                    int correctCount = 0;
                    int expectedCorrectCount = 0;
                    for (Answer a : q.getAnswers()) {
                        if (a.getIsCorrect()) expectedCorrectCount++;
                    }
                    for (String val : submittedValues) {
                        for (Answer a : q.getAnswers()) {
                            if ((a.getId().toString().equals(val) || (a.getLabel() != null && a.getLabel().equalsIgnoreCase(val))) && a.getIsCorrect()) {
                                correctCount++;
                            }
                        }
                    }
                    if (correctCount > 0 && correctCount == submittedValues.length && expectedCorrectCount == submittedValues.length) {
                        isCorrect = true;
                    } else if (correctCount > 0) {
                        // Partial credit logic? Usually IELTS doesn't have partial credit per question, but sometimes 1 mark per correct checkbox.
                        // Let's assume if they got ANY correct but it's a "Choose 2" question where each is 1 mark, it should be 2 separate questions.
                        // For simplicity, if they chose 1 correct out of 2 expected, we give them 1 mark.
                        // Wait, the user asked: "Câu 2 là dạng Multiple choice nhưng vẫn chỉ cho chọn 1 (Chọn 1 trong 2 đáp án đúng thì vẫn tính câu đúng)"
                        // This means if ANY of the submitted values is correct, they get the mark!
                        isCorrect = true; 
                    }
                } else {
                    // Fill in blanks, Matching, Summary
                    String ans = submittedValues[0].trim();
                    if (ans.equalsIgnoreCase(q.getCorrectAnswer())) {
                        isCorrect = true;
                    } else if (q.getAcceptedAnswers() != null) {
                        for (String acc : q.getAcceptedAnswers()) {
                            if (ans.equalsIgnoreCase(acc.trim())) {
                                isCorrect = true;
                                break;
                            }
                        }
                    }
                }

                if (isCorrect) {
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