package com.project.englishlearning.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.entity.WritingSubmission;
import com.project.englishlearning.repository.LessonRepository;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.repository.WritingSubmissionRepository;
import com.project.englishlearning.service.GeminiAiService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/lessons/{lessonId}/writing")
public class StudentWritingController {

    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final GeminiAiService geminiAiService;

    public StudentWritingController(LessonRepository lessonRepository, UserRepository userRepository,
            WritingSubmissionRepository writingSubmissionRepository, GeminiAiService geminiAiService) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.geminiAiService = geminiAiService;
    }

    @GetMapping
    public String takeWritingTest(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        model.addAttribute("lesson", lesson);
        return "student/take-writing";
    }

    @PostMapping("/submit")
    public String submitWriting(@PathVariable Long lessonId,
            @RequestParam String essay,
            Authentication authentication,
            Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();

        String aiResponseJson = geminiAiService.evaluateWriting(lesson.getContent(), essay);

        double bandScore = 0.0;
        String feedback = "Không thể phân tích phản hồi từ AI.";

        String suggestedEssay = "Chưa có bài gợi ý."; 
        try {
            String cleanJson = aiResponseJson.replace("```json", "").replace("```", "").trim();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(cleanJson);

            bandScore = root.path("bandScore").asDouble();
            feedback = root.path("feedback").asText();
            // Lấy thêm bài gợi ý từ AI
            suggestedEssay = root.path("suggestedEssay").asText();
        } catch (Exception e) {
            System.out.println("Lỗi parse JSON: " + e.getMessage());
            System.out.println("Chuỗi gốc: " + aiResponseJson);
        }

        // Lưu toàn bộ vào Database
        WritingSubmission submission = new WritingSubmission();
        submission.setUser(user);
        submission.setLesson(lesson);
        submission.setSubmissionText(essay);
        submission.setBandScore(bandScore);
        submission.setFeedback(feedback);
        submission.setSuggestedEssay(suggestedEssay); // Lưu bài gợi ý
        writingSubmissionRepository.save(submission);

        model.addAttribute("submission", submission);
        model.addAttribute("lesson", lesson);
        return "student/writing-result";
    }
}