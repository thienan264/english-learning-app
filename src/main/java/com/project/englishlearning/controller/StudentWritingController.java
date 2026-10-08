package com.project.englishlearning.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.GeminiAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/lessons/{lessonId}/writing")
public class StudentWritingController {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final WritingExamRepository writingExamRepository;
    private final GeminiAiService geminiAiService;
    private final com.project.englishlearning.service.ExpertWritingService expertWriting;

    public StudentWritingController(LessonRepository lessonRepository, UserRepository userRepository,
            WritingSubmissionRepository writingSubmissionRepository,
            WritingExamRepository writingExamRepository,
            GeminiAiService geminiAiService, com.project.englishlearning.service.ExpertWritingService expertWriting) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.writingExamRepository = writingExamRepository;
        this.geminiAiService = geminiAiService;
        this.expertWriting = expertWriting;
    }

    @GetMapping
    public String takeWritingTest(@PathVariable Long lessonId, Model model, Authentication authentication) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        model.addAttribute("lesson", lesson);
        User viewer = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("expertAvailability", expertWriting.availability(viewer, lesson));

        // Try to find the new WritingExam
        WritingExam exam = writingExamRepository.findByLessonId(lessonId).orElse(null);
        if (exam != null) {
            model.addAttribute("writingExam", exam);
            model.addAttribute("task1", exam.getTask1());
            model.addAttribute("task2", exam.getTask2());
            model.addAttribute("totalTime", exam.getTotalTimeMinutes());
            return "student/take-writing";
        }

        // Fallback: old-style lesson with just content field as the prompt
        model.addAttribute("totalTime", 60);
        return "student/take-writing";
    }

    @PostMapping("/submit")
    @ResponseBody
    public ResponseEntity<?> submitWriting(@PathVariable Long lessonId,
            @RequestParam(required = false) String task1Essay,
            @RequestParam(required = false) String task2Essay,
            @RequestParam(required = false) String essay, // backward compat
            Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        WritingExam exam = writingExamRepository.findByLessonId(lessonId).orElse(null);

        WritingSubmission submission = new WritingSubmission();
        submission.setUser(user);
        submission.setLesson(lesson);
        submission.setWritingExam(exam);
        submission.setStatus("EVALUATING");
        submission.setGradingMode("AI");

        if (exam != null) {
            submission.setTask1Essay(task1Essay);
            submission.setTask2Essay(task2Essay);
            submission.setSubmissionText((task1Essay != null ? task1Essay : "") + "\n---\n" + (task2Essay != null ? task2Essay : ""));
        } else {
            // Backward compatibility
            submission.setSubmissionText(essay != null ? essay : (task1Essay == null ? "" : task1Essay) + "\n\n" + (task2Essay == null ? "" : task2Essay));
        }

        writingSubmissionRepository.save(submission);

        String task1Prompt = "";
        String task2Prompt = "";
        if (exam != null) {
            if (exam.getTask1() != null) task1Prompt = exam.getTask1().getInstruction();
            if (exam.getTask2() != null) task2Prompt = exam.getTask2().getInstruction();
        } else if (lesson.getContent() != null) {
            task2Prompt = lesson.getContent();
        }

        if (exam == null && essay == null) essay = submission.getSubmissionText();

        // Start async AI evaluation
        evaluateAsync(submission.getId(), task1Prompt, task2Prompt, task1Essay, task2Essay, essay);

        return ResponseEntity.accepted().body(Map.of(
                "submissionId", submission.getId(),
                "message", "Bài viết đã được nộp. AI đang chấm điểm..."
        ));
    }

    @GetMapping("/submissions/{submissionId}/status")
    @ResponseBody
    public ResponseEntity<?> checkStatus(@PathVariable Long lessonId, @PathVariable Long submissionId, Authentication authentication) {
        WritingSubmission s = writingSubmissionRepository.findById(submissionId).orElseThrow();
        requireOwner(s, lessonId, authentication);
        return ResponseEntity.ok(Map.of(
                "status", s.getStatus(),
                "submissionId", s.getId()
        ));
    }

    @GetMapping("/result/{submissionId}")
    public String viewResult(@PathVariable Long lessonId, @PathVariable Long submissionId, Model model, Authentication authentication) {
        WritingSubmission submission = writingSubmissionRepository.findById(submissionId).orElseThrow();
        requireOwner(submission, lessonId, authentication);
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        model.addAttribute("submission", submission);
        model.addAttribute("lesson", lesson);
        model.addAttribute("expertAvailability", expertWriting.availability(submission.getUser(), lesson));
        model.addAttribute("expertReview", expertWriting.review(submission));
        return "EXPERT".equals(submission.getGradingMode()) ? "student/expert-writing-result" : "student/writing-result";
    }

    @PostMapping("/expert-submit")
    @ResponseBody
    public ResponseEntity<?> submitExpert(@PathVariable Long lessonId,
            @RequestParam(required=false) String task1Essay, @RequestParam(required=false) String task2Essay,
            @RequestParam(required=false) String essay, Authentication authentication) {
        User user=userRepository.findByUsername(authentication.getName()).orElseThrow();
        Lesson lesson=lessonRepository.findById(lessonId).orElseThrow();
        try {
            WritingSubmission submission=expertWriting.submit(user,lesson,task1Essay,task2Essay,essay);
            return ResponseEntity.accepted().body(Map.of("submissionId",submission.getId(),"message","Đã gửi bài. Vui lòng chờ chuyên gia phản hồi."));
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason() == null ? "Không thể gửi bài." : ex.getReason()));
        }
    }
    private void requireOwner(WritingSubmission submission, Long lessonId, Authentication authentication) {
        if(authentication == null || !submission.getUser().getUsername().equals(authentication.getName()) || !submission.getLesson().getId().equals(lessonId))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
    }

    // ============ ASYNC AI EVALUATION ============

    private void evaluateAsync(Long submissionId, String task1Prompt, String task2Prompt,
                               String task1Essay, String task2Essay, String legacyEssay) {
        // Run in a new thread to avoid blocking the HTTP response
        new Thread(() -> {
            try {
                WritingSubmission submission = writingSubmissionRepository.findById(submissionId).orElseThrow();

                String aiResponseJson = geminiAiService.evaluateWritingDetailed(
                        task1Prompt, task1Essay,
                        task2Prompt, task2Essay != null ? task2Essay : legacyEssay
                );

                // Parse the JSON response
                String cleanJson = aiResponseJson.replace("```json", "").replace("```", "").trim();
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(cleanJson);

                submission.setIsOffTopic(root.path("isOffTopic").asBoolean(false));
                submission.setBandScore(root.path("overallBand").asDouble(0.0));

                // Task 1 scores
                JsonNode t1 = root.path("task1Evaluation");
                if (!t1.isMissingNode()) {
                    submission.setTask1Overall(t1.path("bandScore").asDouble(0));
                    JsonNode c1 = t1.path("criteria");
                    submission.setTask1TaScore(c1.path("taskAchievement").asDouble(0));
                    submission.setTask1CcScore(c1.path("coherenceCohesion").asDouble(0));
                    submission.setTask1LrScore(c1.path("lexicalResource").asDouble(0));
                    submission.setTask1GraScore(c1.path("grammaticalRange").asDouble(0));
                }

                // Task 2 scores
                JsonNode t2 = root.path("task2Evaluation");
                if (!t2.isMissingNode()) {
                    submission.setTask2Overall(t2.path("bandScore").asDouble(0));
                    JsonNode c2 = t2.path("criteria");
                    submission.setTask2TrScore(c2.path("taskResponse").asDouble(0));
                    submission.setTask2CcScore(c2.path("coherenceCohesion").asDouble(0));
                    submission.setTask2LrScore(c2.path("lexicalResource").asDouble(0));
                    submission.setTask2GraScore(c2.path("grammaticalRange").asDouble(0));
                }

                // General feedback
                String feedback = "";
                if (!t1.isMissingNode()) feedback += "TASK 1:\n" + t1.path("generalFeedback").asText("") + "\n\n";
                if (!t2.isMissingNode()) feedback += "TASK 2:\n" + t2.path("generalFeedback").asText("");
                submission.setFeedback(feedback.trim());

                submission.setFeedbackJson(cleanJson);
                submission.setSuggestedEssay(root.path("suggestedEssay").asText(""));
                submission.setStatus("COMPLETED");
                submission.setEvaluatedAt(LocalDateTime.now());
                writingSubmissionRepository.save(submission);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    WritingSubmission submission = writingSubmissionRepository.findById(submissionId).orElseThrow();
                    submission.setStatus("ERROR");
                    submission.setFeedback("Lỗi chấm điểm AI: " + e.getMessage());
                    submission.setBandScore(0.0);
                    submission.setEvaluatedAt(LocalDateTime.now());
                    writingSubmissionRepository.save(submission);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }).start();
    }
}