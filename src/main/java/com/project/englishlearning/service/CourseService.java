package com.project.englishlearning.service;

import com.project.englishlearning.dto.CourseDTO;
import com.project.englishlearning.entity.Course;
import java.util.List;

public interface CourseService {
    // Dành cho Học viên
    List<CourseDTO> getAllCoursesDTO();
    
    // Dành cho Admin
    List<Course> getAllCourses();
    Course getCourseById(Long id);
    void saveCourse(Course course);
    void deleteCourse(Long id);
}