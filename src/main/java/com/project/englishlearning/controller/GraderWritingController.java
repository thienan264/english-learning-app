package com.project.englishlearning.controller;

import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import com.project.englishlearning.dto.ExpertWritingReviewForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
@RequestMapping("/grader")
public class GraderWritingController {
    private final UserRepository users;
    private final WritingSubmissionRepository submissions;
    private final NotificationRepository notifications;
    private final ExpertWritingService service;
    public GraderWritingController(UserRepository users, WritingSubmissionRepository submissions, NotificationRepository notifications, ExpertWritingService service) {
        this.users=users;this.submissions=submissions;this.notifications=notifications;this.service=service;
    }
    private User actor(Authentication auth) { return users.findByUsername(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN)); }
    private void common(Model model, Authentication auth) {
        var user=actor(auth);model.addAttribute("graderView",true);model.addAttribute("reviewBase","/grader/writing/reviews");
        model.addAttribute("graderName",user.getFullName());
        model.addAttribute("graderNotifications",notifications.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).toList());
    }
    @GetMapping({"", "/writing/reviews"})
    public String list(@RequestParam(defaultValue="OPEN") String status, @RequestParam(defaultValue="") String q, Authentication auth, Model model) {
        common(model,auth);var rows=submissions.findByAssignedGraderIdAndGradingModeOrderBySubmittedAtAsc(actor(auth).getId(),"EXPERT");
        model.addAttribute("openCount",rows.stream().filter(s -> WritingAssignmentService.OPEN.contains(s.getStatus())).count());
        model.addAttribute("selectedStatus",status);model.addAttribute("search",q);
        String term=q.strip().toLowerCase(java.util.Locale.ROOT);
        model.addAttribute("submissions",rows.stream().filter(s -> "ALL".equals(status) || ("OPEN".equals(status) ? WritingAssignmentService.OPEN.contains(s.getStatus()) : status.equals(s.getStatus())))
            .filter(s -> (s.getUser().getUsername()+" "+s.getUser().getFullName()+" "+s.getLesson().getTitle()).toLowerCase(java.util.Locale.ROOT).contains(term)).toList());
        return "admin/expert-writing-list";
    }
    @GetMapping("/writing/reviews/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        var s=submissions.findById(id).filter(row -> "EXPERT".equals(row.getGradingMode())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        service.requireReviewer(s,auth.getName());common(model,auth);
        model.addAttribute("submission",s);model.addAttribute("expertReview",service.review(s));model.addAttribute("annotationEditable",true);
        if(!model.containsAttribute("reviewForm")) model.addAttribute("reviewForm",new ExpertWritingReviewForm());
        return "admin/expert-writing-review";
    }
    @PostMapping("/writing/reviews/{id}/start")
    public String start(@PathVariable Long id, Authentication auth) { service.startReview(id,auth.getName());return "redirect:/grader/writing/reviews/"+id; }
    @PostMapping("/writing/reviews/{id}/respond")
    public String respond(@PathVariable Long id,@ModelAttribute("reviewForm") ExpertWritingReviewForm form, Authentication auth,RedirectAttributes redirect) {
        try { service.respondTasks(id,auth.getName(),form);redirect.addFlashAttribute("success","Đã gửi phản hồi. Hệ thống sẽ phân tiếp bài đang chờ nếu còn chỗ."); }
        catch(ResponseStatusException ex) {
            if(ex.getStatusCode().value()==403 || ex.getStatusCode().value()==404) throw ex;
            redirect.addFlashAttribute("error",ex.getReason());redirect.addFlashAttribute("reviewForm",form);
        }
        return "redirect:/grader/writing/reviews/"+id;
    }
    @PostMapping("/notifications/{id}/read")
    public String read(@PathVariable Long id,Authentication auth) {
        var n=notifications.findById(id).filter(row -> row.getUser().getId().equals(actor(auth).getId())).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        n.setIsRead(true);notifications.save(n);
        // Only internal grading links are navigable from this restricted workspace.
        return "redirect:"+(n.getUrl()!=null && n.getUrl().matches("/grader/writing/reviews/[0-9]+") ? n.getUrl() : "/grader/writing/reviews");
    }
}
