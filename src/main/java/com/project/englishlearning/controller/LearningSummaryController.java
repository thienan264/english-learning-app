package com.project.englishlearning.controller;

import com.project.englishlearning.dto.LearningSummary;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.LearningSummaryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Controller
@RequestMapping("/profile/learning-summary")
public class LearningSummaryController {
    private final UserRepository users;
    private final LearningSummaryService summaries;
    private final LessonRepository lessons;
    public LearningSummaryController(UserRepository users,LearningSummaryService summaries,LessonRepository lessons) {
        this.users=users;this.summaries=summaries;this.lessons=lessons;
    }
    private User currentUser(Authentication auth) {
        if(auth==null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return users.findByUsername(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
    @GetMapping
    public String page(Authentication auth,Model model) {
        model.addAttribute("summary",summaries.summarize(currentUser(auth)));
        model.addAttribute("placementLessons",lessons.findByAssessmentRoleOrderByIdAsc("PLACEMENT").stream()
                .filter(l->"READING".equals(l.getSkillType()) || "LISTENING".equals(l.getSkillType())).toList());
        return "student/learning-summary";
    }
    @GetMapping("/data") @ResponseBody
    public LearningSummary data(Authentication auth) { return summaries.summarize(currentUser(auth)); }
}
