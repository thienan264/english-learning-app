package com.project.englishlearning.controller;

import com.project.englishlearning.entity.AdvisoryRequest;
import com.project.englishlearning.repository.AdvisoryRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/advisory")
public class AdvisoryController {

    @Autowired
    private AdvisoryRequestRepository repository;

    @PostMapping("/submit")
    public String submitAdvisory(@ModelAttribute AdvisoryRequest request, RedirectAttributes redirectAttributes) {
        repository.save(request);
        redirectAttributes.addFlashAttribute("successAdvisory", "Đăng ký nhận tư vấn thành công! Chúng tôi sẽ liên hệ với bạn trong thời gian sớm nhất.");
        return "redirect:/#advisory-section";
    }
}
