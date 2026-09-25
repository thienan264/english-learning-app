package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Lesson;
import com.project.englishlearning.repository.LessonRepository;
import com.project.englishlearning.service.QuestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/lessons/{lessonId}/questions")
public class AdminQuestionController {

    private final QuestionService questionService;
    private final LessonRepository lessonRepository;

    public AdminQuestionController(QuestionService questionService, LessonRepository lessonRepository) {
        this.questionService = questionService;
        this.lessonRepository = lessonRepository;
    }


    @GetMapping
    public String listQuestions(@PathVariable Long lessonId, Model model) {
        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null) return "redirect:/admin/courses";

        model.addAttribute("lesson", lesson);
        model.addAttribute("questions", questionService.getQuestionsByLessonId(lessonId));
        return "admin/question-list";
    }

    @PostMapping("/add-multiple-choice")
    public String addMultipleChoice(@PathVariable Long lessonId,
                                    @RequestParam String questionText,
                                    @RequestParam String explanation,
                                    @RequestParam String ans0,
                                    @RequestParam String ans1,
                                    @RequestParam String ans2,
                                    @RequestParam String ans3,
                                    @RequestParam int correctIndex) {
        
        String[] answers = {ans0, ans1, ans2, ans3};
        questionService.createMultipleChoiceQuestion(lessonId, questionText, explanation, answers, correctIndex);
        
        return "redirect:/admin/lessons/" + lessonId + "/questions";
    }
}