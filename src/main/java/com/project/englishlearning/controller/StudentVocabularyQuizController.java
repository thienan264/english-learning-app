package com.project.englishlearning.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.englishlearning.entity.Course;
import com.project.englishlearning.entity.Flashcard;
import com.project.englishlearning.entity.FlashcardTestResult;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.CourseRepository;
import com.project.englishlearning.repository.FlashcardRepository;
import com.project.englishlearning.repository.FlashcardTestResultRepository;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/courses/{courseId}/vocabulary-quiz")
public class StudentVocabularyQuizController {

    private final CourseRepository courseRepository;
    private final FlashcardRepository flashcardRepository;
    private final UserRepository userRepository;
    private final FlashcardTestResultRepository testResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StudentVocabularyQuizController(CourseRepository courseRepository,
                                           FlashcardRepository flashcardRepository,
                                           UserRepository userRepository,
                                           FlashcardTestResultRepository testResultRepository) {
        this.courseRepository = courseRepository;
        this.flashcardRepository = flashcardRepository;
        this.userRepository = userRepository;
        this.testResultRepository = testResultRepository;
    }

    @GetMapping
    public String startQuiz(@PathVariable Long courseId, Model model, Authentication auth) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        List<Flashcard> allCards = flashcardRepository.findByCourseId(courseId);
        
        if (allCards.size() < 4) {
            model.addAttribute("error", "Khóa học cần ít nhất 4 từ vựng để tạo bài kiểm tra.");
            return "redirect:/courses/" + courseId + "/study-flashcards";
        }
        
        // Randomly select up to 10 questions
        Collections.shuffle(allCards);
        List<Flashcard> quizCards = allCards.stream().limit(10).collect(Collectors.toList());
        
        List<Map<String, Object>> questions = new ArrayList<>();
        int i = 1;
        for (Flashcard fc : quizCards) {
            Map<String, Object> q = new HashMap<>();
            q.put("id", fc.getId());
            q.put("word", fc.getWord());
            
            // Generate 4 options: 1 correct, 3 wrong
            List<String> options = new ArrayList<>();
            options.add(fc.getMeaning()); // Correct
            
            List<Flashcard> wrongCards = new ArrayList<>(allCards);
            wrongCards.remove(fc);
            Collections.shuffle(wrongCards);
            options.add(wrongCards.get(0).getMeaning());
            options.add(wrongCards.get(1).getMeaning());
            options.add(wrongCards.get(2).getMeaning());
            
            Collections.shuffle(options);
            q.put("options", options);
            q.put("index", i++);
            questions.add(q);
        }

        model.addAttribute("course", course);
        model.addAttribute("questions", questions);
        return "student/flashcard-quiz";
    }

    @PostMapping("/submit")
    public String submitQuiz(@PathVariable Long courseId, 
                             @RequestParam Map<String, String> allParams,
                             Authentication auth, Model model) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        Course course = courseRepository.findById(courseId).orElseThrow();
        
        List<Flashcard> allCards = flashcardRepository.findByCourseId(courseId);
        Map<Long, Flashcard> cardMap = allCards.stream().collect(Collectors.toMap(Flashcard::getId, fc -> fc));
        
        int totalQuestions = 0;
        int correctAnswers = 0;
        
        List<Map<String, Object>> detailedResults = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith("q_")) {
                totalQuestions++;
                Long flashcardId = Long.parseLong(entry.getKey().substring(2));
                String selectedMeaning = entry.getValue();
                
                Flashcard fc = cardMap.get(flashcardId);
                boolean isCorrect = fc.getMeaning().equals(selectedMeaning);
                if (isCorrect) correctAnswers++;
                
                Map<String, Object> detail = new HashMap<>();
                detail.put("word", fc.getWord());
                detail.put("selected", selectedMeaning);
                detail.put("correct", fc.getMeaning());
                detail.put("isCorrect", isCorrect);
                detailedResults.add(detail);
            }
        }
        
        double score = totalQuestions > 0 ? (double) correctAnswers / totalQuestions * 10.0 : 0;
        score = Math.round(score * 10.0) / 10.0;
        
        FlashcardTestResult result = new FlashcardTestResult();
        result.setUser(user);
        result.setCourse(course);
        result.setTotalQuestions(totalQuestions);
        result.setCorrectAnswers(correctAnswers);
        result.setScore(score);
        try {
            result.setDetailedResultJson(objectMapper.writeValueAsString(detailedResults));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        testResultRepository.save(result);
        
        return "redirect:/courses/" + courseId + "/vocabulary-quiz/result/" + result.getId();
    }
    
    @GetMapping("/result/{resultId}")
    public String viewResult(@PathVariable Long courseId, @PathVariable Long resultId, Authentication auth, Model model) {
        FlashcardTestResult result = testResultRepository.findById(resultId).orElseThrow();
        model.addAttribute("result", result);
        
        try {
            List<?> detailedList = objectMapper.readValue(result.getDetailedResultJson(), List.class);
            model.addAttribute("detailedList", detailedList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "student/flashcard-quiz-result";
    }
}
