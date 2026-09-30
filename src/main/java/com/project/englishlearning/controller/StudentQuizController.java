package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.entity.Question;
import com.project.englishlearning.entity.Answer;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.LessonRepository;
import com.project.englishlearning.repository.QuestionRepository;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.service.StudentLearningService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/lessons/{lessonId}")
public class StudentQuizController {

    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final StudentLearningService learningService;

    public StudentQuizController(LessonRepository lessonRepository, QuestionRepository questionRepository,
                                 UserRepository userRepository, StudentLearningService learningService) {
        this.lessonRepository = lessonRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.learningService = learningService;
    }

    @GetMapping("/take")
    public String takeQuiz(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        List<Question> questions = questionRepository.findByLessonId(lessonId);

        model.addAttribute("lesson", lesson);
        model.addAttribute("questions", questions);
        return "student/take-quiz";
    }

    @PostMapping("/submit-quiz")
    public String submitQuiz(@PathVariable Long lessonId,
                             jakarta.servlet.http.HttpServletRequest request,
                             Authentication authentication,
                             RedirectAttributes ra) {
        
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        List<Question> questions = questionRepository.findByLessonId(lessonId);

        int totalQuestions = questions.size();
        int correctAnswers = 0;

        for (Question q : questions) {
            String submittedValue = request.getParameter("question_" + q.getId());
            if (submittedValue == null || submittedValue.trim().isEmpty()) {
                continue;
            }
            submittedValue = submittedValue.trim();

            String type = q.getQuestionType();
            if (type == null || "MULTIPLE_CHOICE".equals(type) || "MULTIPLE_CHOICE_SINGLE".equals(type)) {
                for (Answer a : q.getAnswers()) {
                    if (a.getIsCorrect() && a.getId().toString().equals(submittedValue)) {
                        correctAnswers++;
                        break;
                    }
                }
            } else if ("TRUE_FALSE_NOT_GIVEN".equals(type) || "FILL_IN_THE_BLANK".equals(type)) {
                if (submittedValue.equalsIgnoreCase(q.getCorrectAnswer())) {
                    correctAnswers++;
                }
            }
        }

        // Calculate score (out of 10)
        double score = totalQuestions > 0 ? ((double) correctAnswers / totalQuestions) * 10.0 : 0.0;
        double passScore = lesson.getPassScore() != null ? lesson.getPassScore() : 0.0;

        ra.addFlashAttribute("score", Math.round(score * 10.0) / 10.0);
        ra.addFlashAttribute("correctAnswers", correctAnswers);
        ra.addFlashAttribute("totalQuestions", totalQuestions);

        if (score >= passScore) {
            learningService.markLessonCompleted(user, lessonId);
            ra.addFlashAttribute("passed", true);
            ra.addFlashAttribute("message", "Chúc mừng! Bạn đã vượt qua bài Quiz và bài học tiếp theo đã được mở khóa.");
        } else {
            ra.addFlashAttribute("passed", false);
            ra.addFlashAttribute("message", "Bạn cần đạt ít nhất " + passScore + " điểm để qua bài. Hãy làm lại nhé!");
        }

        return "redirect:/lessons/" + lessonId + "/quiz-result";
    }

    @GetMapping("/quiz-result")
    public String quizResult(@PathVariable Long lessonId, Model model) {
        model.addAttribute("lesson", lessonRepository.findById(lessonId).orElseThrow());
        return "student/quiz-result";
    }
}

