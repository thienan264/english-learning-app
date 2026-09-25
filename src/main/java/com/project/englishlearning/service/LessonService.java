package com.project.englishlearning.service;

import com.project.englishlearning.entity.Lesson;
import java.util.List;

public interface LessonService {
    List<Lesson> getLessonsByCourseId(Long courseId);
    Lesson saveLesson(Lesson lesson);
}