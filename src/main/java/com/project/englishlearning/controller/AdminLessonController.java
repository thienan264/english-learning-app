package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.service.CourseService;
import com.project.englishlearning.service.LessonService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/courses/{courseId}/lessons")
public class AdminLessonController {

    private final LessonService lessonService;
    private final CourseService courseService;
    private final com.project.englishlearning.repository.WritingTaskRepository writingTaskRepository;
    private final com.project.englishlearning.repository.WritingExamRepository writingExamRepository;

    public AdminLessonController(LessonService lessonService, CourseService courseService,
                                 com.project.englishlearning.repository.WritingTaskRepository writingTaskRepository,
                                 com.project.englishlearning.repository.WritingExamRepository writingExamRepository) {
        this.lessonService = lessonService;
        this.courseService = courseService;
        this.writingTaskRepository = writingTaskRepository;
        this.writingExamRepository = writingExamRepository;
    }

    @GetMapping
    public String listLessons(@PathVariable Long courseId, Model model) {
        Course course = courseService.getCourseById(courseId);
        if (course == null) return "redirect:/admin/courses";
        
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessonService.getLessonsByCourseId(courseId));
        
        model.addAttribute("task1List", writingTaskRepository.findByTaskTypeOrderByCreatedAtDesc("TASK_1"));
        model.addAttribute("task2List", writingTaskRepository.findByTaskTypeOrderByCreatedAtDesc("TASK_2"));
        
        return "admin/lesson-list";
    }

    @PostMapping("/add")
    public String addLesson(@PathVariable Long courseId, 
                            @ModelAttribute Lesson lesson,
                            @RequestParam(required = false) Long task1Id,
                            @RequestParam(required = false) Long task2Id,
                            @RequestParam(required = false) Integer totalTimeMinutes) {
        Course course = courseService.getCourseById(courseId);
        if (course != null) {
            lesson.setCourse(course); // Gắn bài học này vào đúng khóa học
            lesson = lessonService.saveLesson(lesson); // Get saved lesson with ID

            // If it's a WRITING lesson and tasks were selected, create the WritingExam automatically
            if ("WRITING".equals(lesson.getSkillType()) && (task1Id != null || task2Id != null)) {
                com.project.englishlearning.entity.WritingExam exam = new com.project.englishlearning.entity.WritingExam();
                exam.setLesson(lesson);
                exam.setTitle("Writing Test - " + lesson.getTitle());
                if (task1Id != null) exam.setTask1(writingTaskRepository.findById(task1Id).orElse(null));
                if (task2Id != null) exam.setTask2(writingTaskRepository.findById(task2Id).orElse(null));
                exam.setTotalTimeMinutes(totalTimeMinutes != null ? totalTimeMinutes : 60);
                writingExamRepository.save(exam);
            }
        }
        return "redirect:/admin/courses/" + courseId + "/lessons";
    }
}