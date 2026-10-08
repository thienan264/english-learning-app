package com.project.englishlearning.controller;

import com.project.englishlearning.repository.WritingSubmissionRepository;
import com.project.englishlearning.service.ExpertWritingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/writing/reviews")
public class AdminExpertWritingController {
    private final WritingSubmissionRepository submissions;
    private final ExpertWritingService service;
    public AdminExpertWritingController(WritingSubmissionRepository submissions, ExpertWritingService service) {
        this.submissions=submissions;this.service=service;
    }
    @GetMapping
    public String list(@RequestParam(defaultValue="WAITING_REVIEW") String status, Model model) {
        model.addAttribute("selectedStatus",status);
        model.addAttribute("submissions",submissions.findByGradingModeOrderBySubmittedAtDesc("EXPERT").stream()
            .filter(s -> "ALL".equals(status) || status.equals(s.getStatus())).toList());
        return "admin/expert-writing-list";
    }
    @GetMapping("/{id}")
    public String details(@PathVariable Long id, Model model) {
        var s=submissions.findById(id).filter(row -> "EXPERT".equals(row.getGradingMode()))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("submission",s);model.addAttribute("expertReview",service.review(s));model.addAttribute("annotationEditable",true);
        if(!model.containsAttribute("reviewForm")) model.addAttribute("reviewForm",new com.project.englishlearning.dto.ExpertWritingReviewForm());
        return "admin/expert-writing-review";
    }
    @PostMapping("/{id}/respond")
    public String respond(@PathVariable Long id, @ModelAttribute("reviewForm") com.project.englishlearning.dto.ExpertWritingReviewForm form,
            Authentication authentication, RedirectAttributes redirect) {
        try {
            service.respondTasks(id,authentication.getName(),form);
            redirect.addFlashAttribute("success","Đã gửi phản hồi và thông báo cho học viên.");
        } catch(ResponseStatusException ex) {
            if(ex.getStatusCode().value()==404) throw ex;
            redirect.addFlashAttribute("error",ex.getReason());
            redirect.addFlashAttribute("reviewForm",form);
        }
        return "redirect:/admin/writing/reviews/"+id;
    }
}
