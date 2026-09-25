package com.project.englishlearning.controller;

import com.project.englishlearning.dto.UserDTO;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.TestResultRepository;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.repository.WritingSubmissionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class StudentProfileController {

    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;

    public StudentProfileController(UserRepository userRepository, TestResultRepository testResultRepository, WritingSubmissionRepository writingSubmissionRepository) {
        this.userRepository = userRepository;
        this.testResultRepository = testResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
    }

    @GetMapping
    public String viewProfile(Authentication authentication, Model model) {
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow();

        // Chuyển đổi Entity nhạy cảm sang DTO an toàn
        UserDTO safeUserDTO = new UserDTO(user.getId(), user.getUsername(), user.getRole());

        // Đẩy DTO ra giao diện thay vì Entity gốc
        model.addAttribute("user", safeUserDTO);
        
        // Vẫn nạp lịch sử học tập bình thường
        model.addAttribute("testResults", testResultRepository.findByUserIdOrderByCompletedAtDesc(user.getId()));
        model.addAttribute("writingSubmissions", writingSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId()));
        
        return "student/profile";
    }
}