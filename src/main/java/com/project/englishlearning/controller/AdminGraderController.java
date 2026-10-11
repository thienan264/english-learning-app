package com.project.englishlearning.controller;

import com.project.englishlearning.repository.*;
import com.project.englishlearning.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/graders")
public class AdminGraderController {
    private final UserRepository users;private final WritingSubmissionRepository submissions;private final GraderAccountService accounts;
    public AdminGraderController(UserRepository users,WritingSubmissionRepository submissions,GraderAccountService accounts) { this.users=users;this.submissions=submissions;this.accounts=accounts; }
    @GetMapping public String list(Model model) {
        model.addAttribute("graders",users.findAll().stream().filter(u -> "ROLE_GRADER".equals(u.getRole())).toList());
        var loads=new java.util.HashMap<Long,Long>();
        for(var s:submissions.findByGradingModeAndStatusInOrderBySubmittedAtAscIdAsc("EXPERT",WritingAssignmentService.OPEN)) if(s.getAssignedGrader()!=null) loads.merge(s.getAssignedGrader().getId(),1L,Long::sum);
        model.addAttribute("graderLoads",loads);return "admin/graders";
    }
    @PostMapping public String create(@RequestParam String username,@RequestParam String email,@RequestParam String fullName,@RequestParam String password,RedirectAttributes redirect) {
        try { accounts.create(username,email,fullName,password);redirect.addFlashAttribute("success","Đã tạo người chấm và tự động phân bài đang chờ."); }
        catch(org.springframework.web.server.ResponseStatusException ex) { redirect.addFlashAttribute("error",ex.getReason()); }
        catch(org.springframework.dao.DataIntegrityViolationException ex) { redirect.addFlashAttribute("error","Tên tài khoản hoặc email đã tồn tại."); }
        return "redirect:/admin/graders";
    }
    @PostMapping("/{id}/{action}") public String change(@PathVariable Long id,@PathVariable String action,RedirectAttributes redirect) {
        accounts.change(id,action);redirect.addFlashAttribute("success","Đã cập nhật tài khoản và phân lại bài chưa hoàn tất.");return "redirect:/admin/graders";
    }
}
