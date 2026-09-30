package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.repository.LessonRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/exams")
public class AdminExamBankController {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    public AdminExamBankController(CourseRepository courseRepository, LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping
    public String listCoursesForExams(Model model) {
        // Lấy danh sách các khóa học thực sự (bỏ qua khóa học ảo nếu có)
        List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> !"Ngân hàng đề thi (Hệ thống)".equals(c.getTitle()))
                .collect(Collectors.toList());

        model.addAttribute("courses", courses);
        return "admin/exam-bank-courses";
    }

    @GetMapping("/course/{courseId}")
    public String listExamsInCourse(@PathVariable Long courseId, Model model) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        
        // Chỉ lấy các bài học có type là MOCK_TEST trong khóa học này
        List<Lesson> mockTests = lessonRepository.findByCourseId(courseId).stream()
                .filter(l -> "MOCK_TEST".equals(l.getLessonType()))
                .collect(Collectors.toList());

        model.addAttribute("course", course);
        model.addAttribute("mockTests", mockTests);
        return "admin/exam-bank-list";
    }
}
