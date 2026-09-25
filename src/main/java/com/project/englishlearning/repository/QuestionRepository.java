package com.project.englishlearning.repository;

import com.project.englishlearning.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    // Lấy toàn bộ câu hỏi của một bài học
    List<Question> findByLessonId(Long lessonId);
}