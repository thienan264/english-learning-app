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

    public CourseServiceImpl(CourseRepository courseRepository, ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.modelMapper = modelMapper;
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
    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    // --- HÀM CHUYỂN ĐỔI DTO MỚI ---

    @Override
    public List<CourseDTO> getAllCoursesDTO() {
        List<Course> courses = courseRepository.findAll();
        
        return courses.stream()
                .map(course -> modelMapper.map(course, CourseDTO.class))
                .collect(Collectors.toList());
    }
}