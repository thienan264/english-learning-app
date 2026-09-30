package com.project.englishlearning.repository;

import com.project.englishlearning.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;



import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitle(String title);
}