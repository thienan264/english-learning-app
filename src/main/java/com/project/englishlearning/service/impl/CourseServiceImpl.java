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
    private final com.project.englishlearning.service.CourseReviewService reviewService;
    private final com.project.englishlearning.repository.CourseReviewRepository reviewRepository;
    private final ModelMapper modelMapper; 
    private final com.project.englishlearning.repository.LessonRepository lessonRepository;
    private final com.project.englishlearning.repository.FlashcardRepository flashcardRepository;
    private final com.project.englishlearning.repository.TestResultRepository testResultRepository;
    private final com.project.englishlearning.repository.WritingSubmissionRepository writingSubmissionRepository;
    private final com.project.englishlearning.repository.ExamPassageRepository examPassageRepository;
    private final com.project.englishlearning.repository.QuestionGroupRepository questionGroupRepository;
    private final com.project.englishlearning.repository.UserCourseEnrollmentRepository enrollmentRepository;
    private final com.project.englishlearning.repository.UserLessonProgressRepository progressRepository;
    private final com.project.englishlearning.repository.UserFlashcardProgressRepository userFlashcardProgressRepository;

    public CourseServiceImpl(CourseRepository courseRepository, ModelMapper modelMapper,
                             com.project.englishlearning.repository.LessonRepository lessonRepository,
                             com.project.englishlearning.repository.FlashcardRepository flashcardRepository,
                             com.project.englishlearning.repository.TestResultRepository testResultRepository,
                             com.project.englishlearning.repository.WritingSubmissionRepository writingSubmissionRepository,
                             com.project.englishlearning.repository.ExamPassageRepository examPassageRepository,
                             com.project.englishlearning.repository.QuestionGroupRepository questionGroupRepository,
                             com.project.englishlearning.repository.UserCourseEnrollmentRepository enrollmentRepository,
                             com.project.englishlearning.repository.UserLessonProgressRepository progressRepository,
                             com.project.englishlearning.repository.UserFlashcardProgressRepository userFlashcardProgressRepository,
                             com.project.englishlearning.service.CourseReviewService reviewService,
                             com.project.englishlearning.repository.CourseReviewRepository reviewRepository) {
        this.courseRepository = courseRepository;
        this.reviewService = reviewService;
        this.reviewRepository = reviewRepository;
        this.modelMapper = modelMapper;
        this.lessonRepository = lessonRepository;
        this.flashcardRepository = flashcardRepository;
        this.testResultRepository = testResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.examPassageRepository = examPassageRepository;
        this.questionGroupRepository = questionGroupRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.progressRepository = progressRepository;
        this.userFlashcardProgressRepository = userFlashcardProgressRepository;
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
        reviewRepository.deleteByCourseId(id);
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
        
        var stats = reviewService.statistics();
        return courses.stream()
                .map(course -> withStats(mapToCourseDTO(course, null), stats))
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CourseDTO> getAllCoursesDTOForUser(Long userId) {
        List<Course> courses = courseRepository.findAll().stream()
                .filter(c -> !"Ngân hàng đề thi (Hệ thống)".equals(c.getTitle()))
                .collect(Collectors.toList());
        
        var stats = reviewService.statistics();
        return courses.stream()
                .map(course -> withStats(mapToCourseDTO(course, userId), stats))
                .collect(Collectors.toList());
    }

    private CourseDTO withStats(CourseDTO dto, java.util.Map<Long, com.project.englishlearning.service.CourseReviewService.Stats> stats) {
        var value = stats.getOrDefault(dto.getId(), new com.project.englishlearning.service.CourseReviewService.Stats(0,0,0));
        dto.setPurchaseCount(value.purchases()); dto.setAverageRating(value.rating()); dto.setReviewCount(value.reviews());
        return dto;
    }

    private CourseDTO mapToCourseDTO(Course course, Long userId) {
        CourseDTO dto = modelMapper.map(course, CourseDTO.class);
        if (course.getCreatedAt() != null && course.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(7))) {
            dto.setIsNewCourse(true);
        }
        
        // Check sale dates
        if (course.getSaleStartDate() != null || course.getSaleEndDate() != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            boolean started = course.getSaleStartDate() == null || !now.isBefore(course.getSaleStartDate());
            boolean ended = course.getSaleEndDate() != null && now.isAfter(course.getSaleEndDate());
            if (!started || ended) {
                dto.setSalePrice(null); // Sale is not active
                dto.setSaleEndDate(null);
            }
        }
        
        if (userId != null) {
            com.project.englishlearning.entity.UserCourseEnrollment enrollment = 
                enrollmentRepository.findByUserIdAndCourseId(userId, course.getId()).orElse(null);
            
            if (enrollment != null && !"REVOKED".equals(enrollment.getStatus())) {
                dto.setIsEnrolled(true);
                dto.setCompletionPercentage(enrollment.getCompletionPercentage());
                dto.setExpiresAt(enrollment.getExpiresAt());
                
                if (enrollment.getExpiresAt() != null) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    if (now.isAfter(enrollment.getExpiresAt())) {
                        dto.setIsExpired(true);
                        dto.setDaysUntilExpiration(0L);
                    } else {
                        dto.setIsExpired(false);
                        dto.setDaysUntilExpiration(java.time.temporal.ChronoUnit.DAYS.between(now, enrollment.getExpiresAt()));
                    }
                } else {
                    dto.setIsExpired(false);
                }
                
                int totalLessons = 0;
                int completedLessons = 0;
                
                List<com.project.englishlearning.entity.UserLessonProgress> progressList = 
                    progressRepository.findByUserId(userId);
                java.util.Map<Long, String> progressMap = progressList.stream()
                    .collect(Collectors.toMap(p -> p.getLesson().getId(), com.project.englishlearning.entity.UserLessonProgress::getStatus));
                
                List<com.project.englishlearning.entity.Lesson> lessons = lessonRepository.findByCourseId(course.getId());
                for (com.project.englishlearning.entity.Lesson l : lessons) {
                    totalLessons++;
                    if ("COMPLETED".equals(progressMap.get(l.getId()))) {
                        completedLessons++;
                    }
                }
                dto.setTotalLessons(totalLessons);
                dto.setCompletedLessons(completedLessons);
                dto.setTotalFlashcards((int) flashcardRepository.countByCourseId(course.getId()));
                dto.setCompletedFlashcards((int) userFlashcardProgressRepository.countByUserIdAndFlashcardCourseIdAndIsFlippedTrue(userId, course.getId()));
            } else {
                dto.setIsEnrolled(false);
            }
        }
        return dto;
    }
}