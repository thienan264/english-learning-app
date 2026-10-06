package com.project.englishlearning.repository;

import com.project.englishlearning.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTitle(String title);

    @Query("SELECT c FROM Course c WHERE c.salePrice IS NOT NULL AND c.isFree = false")
    List<Course> findCoursesWithSalePrice();
}