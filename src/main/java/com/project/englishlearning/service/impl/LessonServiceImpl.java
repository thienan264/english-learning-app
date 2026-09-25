package com.project.englishlearning.service.impl;

import com.project.englishlearning.dto.LessonDTO;
import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.repository.LessonRepository;
import com.project.englishlearning.service.LessonService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;

    public LessonServiceImpl(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    @Override
    public List<Lesson> getLessonsByCourseId(Long courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    @Override
    public Lesson saveLesson(Lesson lesson) {
        return lessonRepository.save(lesson);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonDTO getLessonDTOById(Long id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài học với id: " + id));
        return new LessonDTO(lesson.getId(), lesson.getTitle(), lesson.getContent());
    }
}