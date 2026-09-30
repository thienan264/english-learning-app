package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Module;
import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.repository.ModuleRepository;
import com.project.englishlearning.repository.LessonRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/courses/{courseId}/builder")
public class AdminCourseBuilderController {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    public AdminCourseBuilderController(CourseRepository courseRepository,
                                        ModuleRepository moduleRepository,
                                        LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping
    public String courseBuilder(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        List<Module> modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        
        // Ensure lessons are sorted for each module (if not done in entity)
        for (Module module : modules) {
            module.getLessons().sort((l1, l2) -> {
                Integer idx1 = l1.getOrderIndex() != null ? l1.getOrderIndex() : 0;
                Integer idx2 = l2.getOrderIndex() != null ? l2.getOrderIndex() : 0;
                return idx1.compareTo(idx2);
            });
        }

        model.addAttribute("course", course);
        model.addAttribute("modules", modules);
        return "admin/course-builder";
    }

    @PostMapping("/modules/add")
    public String addModule(@PathVariable Long courseId, @RequestParam String title, RedirectAttributes ra) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        Module module = new Module();
        module.setCourse(course);
        module.setTitle(title);
        
        // Find max order index
        List<Module> existing = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        int nextIdx = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getOrderIndex() + 1;
        module.setOrderIndex(nextIdx);
        
        moduleRepository.save(module);
        ra.addFlashAttribute("success", "Đã thêm Chương mới!");
        return "redirect:/admin/courses/" + courseId + "/builder";
    }

    @PostMapping("/modules/{moduleId}/lessons/add")
    public String addLesson(@PathVariable Long courseId, 
                            @PathVariable Long moduleId,
                            @RequestParam String title,
                            @RequestParam String lessonType,
                            @RequestParam(required = false) String skillType,
                            RedirectAttributes ra) {
        Module module = moduleRepository.findById(moduleId).orElseThrow();
        
        Lesson lesson = new Lesson();
        lesson.setModule(module);
        lesson.setCourse(module.getCourse()); // Backward compatibility
        lesson.setTitle(title);
        lesson.setLessonType(lessonType);
        
        if ("MOCK_TEST".equals(lessonType) && skillType != null && !skillType.isEmpty()) {
            lesson.setSkillType(skillType);
        } else {
            lesson.setSkillType(lessonType.equals("THEORY") ? "READING" : "WRITING"); // Fallback
        }
        
        int nextIdx = module.getLessons().isEmpty() ? 1 : module.getLessons().size() + 1;
        lesson.setOrderIndex(nextIdx);
        
        lessonRepository.save(lesson);
        ra.addFlashAttribute("success", "Đã thêm Bài học mới!");
        return "redirect:/admin/courses/" + courseId + "/builder";
    }

    @PostMapping("/modules/{moduleId}/delete")
    public String deleteModule(@PathVariable Long courseId, @PathVariable Long moduleId, RedirectAttributes ra) {
        moduleRepository.deleteById(moduleId);
        ra.addFlashAttribute("success", "Đã xóa Chương!");
        return "redirect:/admin/courses/" + courseId + "/builder";
    }

    @PostMapping("/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long courseId, @PathVariable Long lessonId, RedirectAttributes ra) {
        lessonRepository.deleteById(lessonId);
        ra.addFlashAttribute("success", "Đã xóa Bài học!");
        return "redirect:/admin/courses/" + courseId + "/builder";
    }
}
