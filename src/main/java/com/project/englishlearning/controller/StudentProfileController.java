package com.project.englishlearning.controller;

import com.project.englishlearning.dto.UserDTO;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.TestResultRepository;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.repository.WritingSubmissionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class StudentProfileController {

    private final UserRepository userRepository;
    private final TestResultRepository testResultRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final org.modelmapper.ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public StudentProfileController(UserRepository userRepository, 
                                    TestResultRepository testResultRepository, 
                                    WritingSubmissionRepository writingSubmissionRepository,
                                    org.modelmapper.ModelMapper modelMapper,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.testResultRepository = testResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String viewProfile(Authentication authentication, Model model) {
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow();

        UserDTO safeUser = modelMapper.map(user, UserDTO.class);

        model.addAttribute("user", safeUser); 
        model.addAttribute("testResults", testResultRepository.findByUserIdOrderByCompletedAtDesc(user.getId()));
        model.addAttribute("writingSubmissions", writingSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId()));
        
        return "student/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam("fullName") String fullName, 
                                @RequestParam("email") String email,
                                Authentication authentication, 
                                RedirectAttributes redirectAttributes) {
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow();

        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng bởi người dùng khác.");
            return "redirect:/profile";
        }

        user.setFullName(fullName);
        user.setEmail(email);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công.");
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow();

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("pwdError", "Mật khẩu cũ không chính xác.");
            return "redirect:/profile";
        }

        if (newPassword == null || !isStrongPassword(newPassword)) {
            redirectAttributes.addFlashAttribute("pwdError", "Mật khẩu mới phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt.");
            return "redirect:/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("pwdError", "Mật khẩu xác nhận không khớp.");
            return "redirect:/profile";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("pwdSuccess", "Đổi mật khẩu thành công.");
        return "redirect:/profile";
    }

    private boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]).{8,}$";
        return password.matches(regex);
    }
}