package com.project.englishlearning.repository;

import com.project.englishlearning.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;



public interface CourseRepository extends JpaRepository<Course, Long> {
}