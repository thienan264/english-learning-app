package com.project.englishlearning.controller;

import com.project.englishlearning.entity.AdvisoryRequest;
import com.project.englishlearning.repository.AdvisoryRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/advisories")
public class AdminAdvisoryController {

    @Autowired
    private AdvisoryRequestRepository repository;

    @GetMapping
    public String listAdvisories(Model model) {
        model.addAttribute("advisories", repository.findAllByOrderByCreatedAtDesc());
        return "admin/advisory-list";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam String status, RedirectAttributes redirectAttributes) {
        AdvisoryRequest request = repository.findById(id).orElse(null);
        if (request != null) {
            request.setStatus(status);
            repository.save(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái thành công!");
        }
        return "redirect:/admin/advisories";
    }
}
