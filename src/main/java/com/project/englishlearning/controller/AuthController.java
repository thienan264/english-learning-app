package com.project.englishlearning.controller;

import com.project.englishlearning.dto.RegisterDTO;
import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; 
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute("registerDTO") RegisterDTO registerDTO, Model model) {
        boolean hasError = false;
        
        if (registerDTO.getUsername() == null || registerDTO.getUsername().length() < 3 || registerDTO.getUsername().length() > 50) {
            model.addAttribute("error", "Tên đăng nhập phải từ 3 đến 50 ký tự.");
            hasError = true;
        } else if (userRepository.existsByUsername(registerDTO.getUsername())) {
            model.addAttribute("error", "Tên đăng nhập đã được sử dụng.");
            hasError = true;
        } else if (registerDTO.getEmail() == null || !registerDTO.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("error", "Email không hợp lệ.");
            hasError = true;
        } else if (userRepository.existsByEmail(registerDTO.getEmail())) {
            model.addAttribute("error", "Email đã được sử dụng.");
            hasError = true;
        } else if (registerDTO.getPassword() == null || !isStrongPassword(registerDTO.getPassword())) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt (!@#$%^&*...).");
            hasError = true;
        } else if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            hasError = true;
        } else if (registerDTO.getFullName() == null || registerDTO.getFullName().length() < 2 || registerDTO.getFullName().length() > 100) {
            model.addAttribute("error", "Họ và tên phải từ 2 đến 100 ký tự.");
            hasError = true;
        }

        if (hasError) {
            return "register";
        }

        User newUser = new User();
        newUser.setUsername(registerDTO.getUsername());
        newUser.setEmail(registerDTO.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        newUser.setFullName(registerDTO.getFullName());
        newUser.setRole("ROLE_STUDENT");
        userRepository.save(newUser);

        return "redirect:/login?registered";
    }

    /**
     * Kiểm tra mật khẩu mạnh theo chuẩn hiện đại:
     * - Ít nhất 8 ký tự
     * - Có ít nhất 1 chữ hoa (A-Z)
     * - Có ít nhất 1 chữ thường (a-z)
     * - Có ít nhất 1 chữ số (0-9)
     * - Có ít nhất 1 ký tự đặc biệt (!@#$%^&* v.v.)
     */
    private boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]).{8,}$";
        return password.matches(regex);
    }
}