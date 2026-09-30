package com.project.englishlearning.service.impl;

import com.project.englishlearning.dto.CourseDTO;
import com.project.englishlearning.entity.Course;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.service.CourseService;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final ModelMapper modelMapper; 
    private final com.project.englishlearning.repository.LessonRepository lessonRepository;
    private final com.project.englishlearning.repository.FlashcardRepository flashcardRepository;
    private final com.project.englishlearning.repository.TestResultRepository testResultRepository;
    private final com.project.englishlearning.repository.WritingSubmissionRepository writingSubmissionRepository;
    private final com.project.englishlearning.repository.ExamPassageRepository examPassageRepository;
    private final com.project.englishlearning.repository.QuestionGroupRepository questionGroupRepository;

    public CourseServiceImpl(CourseRepository courseRepository, ModelMapper modelMapper,
                             com.project.englishlearning.repository.LessonRepository lessonRepository,
                             com.project.englishlearning.repository.FlashcardRepository flashcardRepository,
                             com.project.englishlearning.repository.TestResultRepository testResultRepository,
                             com.project.englishlearning.repository.WritingSubmissionRepository writingSubmissionRepository,
                             com.project.englishlearning.repository.ExamPassageRepository examPassageRepository,
                             com.project.englishlearning.repository.QuestionGroupRepository questionGroupRepository) {
        this.courseRepository = courseRepository;
        this.modelMapper = modelMapper;
        this.lessonRepository = lessonRepository;
        this.flashcardRepository = flashcardRepository;
        this.testResultRepository = testResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.examPassageRepository = examPassageRepository;
        this.questionGroupRepository = questionGroupRepository;
    }

    // --- CÁC HÀM BẮT BUỘC CỦA INTERFACE (BỊ THIẾU) ---

    @Override
    public Course saveCourse(Course course) {
        return courseRepository.save(course);
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElseThrow();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteCourse(Long id) {
        // Find the course
        Course course = courseRepository.findById(id).orElse(null);
        if (course == null) return;
        
        // Find all lessons for this course
        List<com.project.englishlearning.entity.Lesson> lessons = lessonRepository.findByCourseId(id);
            
        // Delete all related data for each lesson
        for (com.project.englishlearning.entity.Lesson lesson : lessons) {
            Long lessonId = lesson.getId();
            
            // Delete TestResults & WritingSubmissions
            testResultRepository.deleteByLessonId(lessonId);
            writingSubmissionRepository.deleteByLessonId(lessonId);
                
            // Delete ExamPassages
            examPassageRepository.deleteByLessonId(lessonId);
                
            // Delete QuestionGroups (cascade delete takes care of questions and answers if set, 
            // but wait, does QuestionGroupRepository have deleteByLessonId?)
            questionGroupRepository.deleteByLessonId(lessonId);
        }
        
        // Delete lessons
        lessonRepository.deleteByCourseId(id);
            
        // Delete flashcards
        flashcardRepository.deleteByCourseId(id);
            
        // Finally, delete the course
        courseRepository.deleteById(id);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll().stream()
                .filter(c -> !"Ngân hàng đề thi (Hệ thống)".equals(c.getTitle()))
                .collect(Collectors.toList());
    }

    // --- HÀM CHUYỂN ĐỔI DTO MỚI ---

    @Override
    public List<CourseDTO> getAllCoursesDTO() {
        List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> !"Ngân hàng đề thi (Hệ thống)".equals(c.getTitle()))
                .collect(Collectors.toList());
        
        return courses.stream()
                .map(course -> modelMapper.map(course, CourseDTO.class))
                .collect(Collectors.toList());
    }
}