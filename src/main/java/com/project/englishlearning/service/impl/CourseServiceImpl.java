package com.project.englishlearning.service.impl; // Đã sửa đúng tên thư mục impl

import com.project.englishlearning.dto.CourseDTO;
import com.project.englishlearning.entity.Course;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.service.CourseService; // Đã bổ sung dòng import Interface này
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    public CourseServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    // --- HÀM DÀNH CHO HỌC VIÊN (DÙNG DTO) ---
    @Override
    public List<CourseDTO> getAllCoursesDTO() {
        return courseRepository.findAll().stream().map(course -> 
            new CourseDTO(
                course.getId(), 
                course.getTitle(), 
                course.getDescription(), 
                course.getLevel()
            )
        ).collect(Collectors.toList());
    }

    // --- CÁC HÀM DÀNH CHO ADMIN (DÙNG ENTITY) ---
    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Override
    public Course getCourseById(Long id) {
        return courseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khóa học"));
    }

    @Override
    public void saveCourse(Course course) {
        courseRepository.save(course);
    }

    @Override
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }
}