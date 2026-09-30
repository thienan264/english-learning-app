package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.entity.LessonTheory;
import com.project.englishlearning.repository.LessonRepository;
import com.project.englishlearning.repository.LessonTheoryRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/admin/lessons/{lessonId}/theory")
public class AdminLessonTheoryController {

    private final LessonRepository lessonRepository;
    private final LessonTheoryRepository lessonTheoryRepository;

    public AdminLessonTheoryController(LessonRepository lessonRepository, LessonTheoryRepository lessonTheoryRepository) {
        this.lessonRepository = lessonRepository;
        this.lessonTheoryRepository = lessonTheoryRepository;
    }

    @GetMapping
    public String editTheory(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        LessonTheory theory = lessonTheoryRepository.findByLessonId(lessonId).orElse(new LessonTheory());
        
        model.addAttribute("lesson", lesson);
        model.addAttribute("theory", theory);
        return "admin/lesson-theory-form";
    }

    @PostMapping("/save")
    public String saveTheory(@PathVariable Long lessonId,
                             @RequestParam String htmlContent,
                             @RequestParam(required = false) String videoUrl,
                             @RequestParam(required = false) MultipartFile documentFile,
                             RedirectAttributes ra) {
        try {
            Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
            LessonTheory theory = lessonTheoryRepository.findByLessonId(lessonId).orElse(new LessonTheory());
            theory.setLesson(lesson);
            theory.setHtmlContent(htmlContent);
            theory.setVideoUrl(videoUrl);

            // Handle file upload
            if (documentFile != null && !documentFile.isEmpty()) {
                String uploadsDir = System.getProperty("user.dir") + "/uploads/documents/";
                File dir = new File(uploadsDir);
                if (!dir.exists()) dir.mkdirs();

                String originalName = documentFile.getOriginalFilename();
                String fileName = System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9.\\-]", "_");
                File dest = new File(dir, fileName);
                Files.copy(documentFile.getInputStream(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                theory.setDocumentUrl("/uploads/documents/" + fileName);
                theory.setDocumentName(originalName);
            }

            lessonTheoryRepository.save(theory);
            ra.addFlashAttribute("success", "Đã lưu nội dung lý thuyết thành công!");
            
            // Redirect back to Course Builder
            return "redirect:/admin/courses/" + lesson.getModule().getCourse().getId() + "/builder";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi lưu nội dung: " + e.getMessage());
            return "redirect:/admin/lessons/" + lessonId + "/theory";
        }
    }
}
