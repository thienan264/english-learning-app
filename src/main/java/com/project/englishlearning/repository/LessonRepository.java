package com.project.englishlearning.repository;

import com.project.englishlearning.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByModuleCourseIdOrderByModuleOrderIndexAscOrderIndexAsc(Long courseId);
    List<Lesson> findByAssessmentRoleOrderByIdAsc(String assessmentRole);
    List<Lesson> findByCourseIdOrderByOrderIndexAsc(Long courseId);
    List<Lesson> findByCourseId(Long courseId);
    void deleteByCourseId(Long courseId);
}