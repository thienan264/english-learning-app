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
}
