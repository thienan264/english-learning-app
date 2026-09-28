package com.project.englishlearning.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class TestController {

    @Autowired
    private AdminExamController adminExamController;

    @GetMapping("/test-error")
    public String testError() {
        try {
            // Find a lesson ID that exists
            adminExamController.getExamData(1L);
            return "Success 1";
        } catch (Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(sw));
            return sw.toString();
        }
    }
}
