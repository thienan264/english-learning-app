package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.repository.LessonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Set;

@Controller
@RequestMapping("/admin/courses/{courseId}/learning-metadata")
public class AdminLearningMetadataController {
    private final CourseRepository courses;
    private final LessonRepository lessons;

    public AdminLearningMetadataController(CourseRepository courses, LessonRepository lessons) {
        this.courses = courses;
        this.lessons = lessons;
    }

    @GetMapping
    public String list(@PathVariable Long courseId, Model model) {
        model.addAttribute("course", courses.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
        model.addAttribute("lessons", lessons.findByModuleCourseIdOrderByModuleOrderIndexAscOrderIndexAsc(courseId));
        return "admin/learning-metadata";
    }

    @PostMapping("/{lessonId}/inline")
    @ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String,String>> saveInline(
            @PathVariable Long courseId,@PathVariable Long lessonId,
            @RequestParam String learningLevel,@RequestParam String assessmentRole,@RequestParam String learningObjective) {
        var flash=new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();
        save(courseId,lessonId,learningLevel,assessmentRole,learningObjective,flash);
        var error=flash.getFlashAttributes().get("error");
        return org.springframework.http.ResponseEntity.status(error==null?200:400)
                .body(java.util.Map.of("message",String.valueOf(error==null?flash.getFlashAttributes().get("success"):error)));
    }

    @PostMapping("/{lessonId}")
    public String save(@PathVariable Long courseId, @PathVariable Long lessonId,
                       @RequestParam String learningLevel, @RequestParam String assessmentRole,
                       @RequestParam String learningObjective, RedirectAttributes flash) {
        Lesson lesson = lessons.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (lesson.getModule() == null || lesson.getModule().getCourse() == null || !courseId.equals(lesson.getModule().getCourse().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String level = learningLevel.strip();
        String role = assessmentRole.strip();
        String objective = learningObjective.strip();
        if (!Set.of("", "BEGINNER", "INTERMEDIATE", "ADVANCED").contains(level)
                || !Set.of("", "PRACTICE", "PLACEMENT", "FINAL").contains(role)
                || objective.length() > 500
                || ("THEORY".equals(lesson.getLessonType()) && !role.isEmpty() && !"PRACTICE".equals(role))) {
            flash.addFlashAttribute("error", "Thông tin chưa hợp lệ. Mục tiêu tối đa 500 ký tự; bài lý thuyết không dùng để đánh giá đầu vào hoặc cuối khóa.");
            return "redirect:/admin/courses/" + courseId + "/learning-metadata";
        }
        lesson.setLearningLevel(level.isEmpty() ? null : level);
        lesson.setAssessmentRole(role.isEmpty() ? null : role);
        lesson.setLearningObjective(objective.isEmpty() ? null : objective);
        lessons.save(lesson);
        flash.addFlashAttribute("success", "Đã lưu mục tiêu học tập: " + lesson.getTitle());
        return "redirect:/admin/courses/" + courseId + "/learning-metadata";
    }
}
