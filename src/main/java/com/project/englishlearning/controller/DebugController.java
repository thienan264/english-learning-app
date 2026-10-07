package com.project.englishlearning.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.project.englishlearning.repository.*;

@RestController
@RequestMapping("/debug")
public class DebugController {
    @Autowired private QuestionGroupRepository groupRepo;
    @Autowired private ExamPassageRepository passageRepo;

    @GetMapping("/db")
    public ResponseEntity<?> testDb() {
        try {
            groupRepo.findAll();
            passageRepo.findAll();
            return ResponseEntity.ok("DB OK");
        } catch (Throwable t) {
            java.io.StringWriter sw = new java.io.StringWriter();
            t.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.ok(sw.toString());
        }
    }

    @Autowired private org.thymeleaf.TemplateEngine templateEngine;
    @Autowired private com.project.englishlearning.service.CourseService courseService;

    @GetMapping("/index")
    public ResponseEntity<?> debugIndex() {
        try {
            org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
            context.setVariable("courses", courseService.getAllCoursesDTO());
            return ResponseEntity.ok(templateEngine.process("index", context));
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return ResponseEntity.internalServerError().body(sw.toString());
        }
    }
}
