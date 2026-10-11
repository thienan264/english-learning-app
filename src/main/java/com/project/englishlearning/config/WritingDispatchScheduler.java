package com.project.englishlearning.config;

import com.project.englishlearning.service.WritingAssignmentService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class WritingDispatchScheduler {
    private final WritingAssignmentService assignments;
    public WritingDispatchScheduler(WritingAssignmentService assignments) { this.assignments=assignments; }
    @Scheduled(fixedDelayString="${writing.assignment.poll-ms:60000}", initialDelayString="${writing.assignment.initial-delay-ms:15000}")
    public void distributeQueue() { assignments.dispatch(); }
}
