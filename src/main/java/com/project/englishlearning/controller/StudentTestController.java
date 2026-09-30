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
        
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ArrayNode detailsArray = objectMapper.createArrayNode();

        for (QuestionGroup g : groups) {
            for (Question q : g.getQuestions()) {
                totalQuestions++;
                String questionKey = "question_" + q.getId();
                String[] submittedValues = request.getParameterValues(questionKey);
                
                String submittedStr = "";
                if (submittedValues != null && submittedValues.length > 0) {
                    submittedStr = String.join(", ", submittedValues);
                }
                
                String qType = q.getQuestionType() != null ? q.getQuestionType() : g.getQuestionType().name();
                boolean isCorrect = false;
                String correctAnswerText = "";

                if (submittedValues == null || submittedValues.length == 0 || submittedValues[0].trim().isEmpty()) {
                    // Just record empty and find correct answer
                } else {
                    if ("MULTIPLE_CHOICE_SINGLE".equals(qType) || "TRUE_FALSE_NOT_GIVEN".equals(qType)) {
                        String ans = submittedValues[0].trim();
                        if ("MULTIPLE_CHOICE_SINGLE".equals(qType)) {
                            for (Answer a : q.getAnswers()) {
                                if (a.getIsCorrect() && (a.getId().toString().equals(ans) || (a.getLabel() != null && a.getLabel().equalsIgnoreCase(ans)))) {
                                    isCorrect = true;
                                    break;
                                }
                            }
                        } else {
                            if (ans.equalsIgnoreCase(q.getCorrectAnswer())) isCorrect = true;
                        }
                    } else if ("MULTIPLE_CHOICE_MULTI".equals(qType)) {
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
                            isCorrect = true; 
                        }
                    } else {
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
                }
                
                // Get correct answer text for recording
                if ("MULTIPLE_CHOICE_SINGLE".equals(qType) || "MULTIPLE_CHOICE_MULTI".equals(qType)) {
                    StringBuilder sb = new StringBuilder();
                    for (Answer a : q.getAnswers()) {
                        if (a.getIsCorrect()) {
                            if (sb.length() > 0) sb.append(", ");
                            sb.append(a.getAnswerText() != null ? a.getAnswerText() : a.getLabel());
                        }
                    }
                    correctAnswerText = sb.toString();
                } else {
                    correctAnswerText = q.getCorrectAnswer();
                    if (q.getAcceptedAnswers() != null && !q.getAcceptedAnswers().isEmpty()) {
                        correctAnswerText += " (hoặc: " + String.join(", ", q.getAcceptedAnswers()) + ")";
                    }
                }

                if (isCorrect) correctAnswers++;
                
                com.fasterxml.jackson.databind.node.ObjectNode detailNode = objectMapper.createObjectNode();
                detailNode.put("questionId", q.getId());
                detailNode.put("questionText", q.getQuestionText());
                detailNode.put("submittedValue", submittedStr);
                detailNode.put("correctAnswer", correctAnswerText);
                detailNode.put("isCorrect", isCorrect);
                detailNode.put("explanation", q.getExplanation());
                detailsArray.add(detailNode);
            }
        }

        double bandScore = ieltsScoringService.calculateBandScore(correctAnswers, totalQuestions);
        TestResult result = new TestResult();
        result.setUser(user);
        result.setLesson(lesson);
        result.setTotalQuestions(totalQuestions);
        result.setCorrectAnswers(correctAnswers);
        result.setScore(bandScore);
        try {
            result.setDetailedResultJson(objectMapper.writeValueAsString(detailsArray));
        } catch (Exception e) {}
        testResultRepository.save(result);

        model.addAttribute("result", result);
        model.addAttribute("lesson", lesson);
        return "student/test-result";
    }

    @GetMapping("/{lessonId}/result/{resultId}")
    public String viewTestResult(@PathVariable Long lessonId, @PathVariable Long resultId, Model model, Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        TestResult result = testResultRepository.findById(resultId).orElse(null);
        if (result == null || !result.getUser().getId().equals(user.getId())) {
            return "redirect:/";
        }
        
        List<Map<String, Object>> detailedAnswers = null;
        if (result.getDetailedResultJson() != null && !result.getDetailedResultJson().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                detailedAnswers = mapper.readValue(result.getDetailedResultJson(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>(){});
            } catch (Exception e) {}
        }
        
        model.addAttribute("result", result);
        model.addAttribute("lesson", result.getLesson());
        model.addAttribute("detailedAnswers", detailedAnswers);
        return "student/test-result-detail";
    }
}