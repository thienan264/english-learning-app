package com.project.englishlearning.service;

import com.project.englishlearning.entity.Course;
import java.util.List;

public interface CourseService {
    List<Course> getAllCourses();
    Course saveCourse(Course course);
    Course getCourseById(Long id);
}