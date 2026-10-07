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
    private final com.project.englishlearning.service.EmailVerificationService emailVerification;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          com.project.englishlearning.service.EmailVerificationService emailVerification) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerification = emailVerification;
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
    public String processRegister(@ModelAttribute("registerDTO") RegisterDTO registerDTO, Model model,
                                  jakarta.servlet.http.HttpSession session) {
        registerDTO.setEmail(com.project.englishlearning.service.EmailVerificationService.normalize(registerDTO.getEmail()));
        if (registerDTO.getUsername() != null) registerDTO.setUsername(registerDTO.getUsername().strip());
        if (registerDTO.getFullName() != null) registerDTO.setFullName(registerDTO.getFullName().strip());
        boolean hasError = false;
        
        if (registerDTO.getUsername() == null || registerDTO.getUsername().length() < 3 || registerDTO.getUsername().length() > 50) {
            model.addAttribute("error", "Tên đăng nhập phải từ 3 đến 50 ký tự.");
            hasError = true;
        } else if (userRepository.existsByUsername(registerDTO.getUsername())) {
            model.addAttribute("error", "Tên đăng nhập đã được sử dụng.");
            hasError = true;
        } else if (!com.project.englishlearning.service.EmailVerificationService.validEmail(registerDTO.getEmail())) {
            model.addAttribute("error", "Email không hợp lệ.");
            hasError = true;
        } else if (userRepository.existsByEmailIgnoreCase(registerDTO.getEmail())) {
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
        try {
            String verificationError = emailVerification.createVerifiedUser(newUser, registerDTO.getVerificationCode(), session.getId());
            if (verificationError != null) {
                model.addAttribute("error", verificationError);
                return "register";
            }
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            model.addAttribute("error", "Tên đăng nhập hoặc email đã được sử dụng. Vui lòng kiểm tra lại.");
            return "register";
        }

        return "redirect:/login?registered";
    }

    @PostMapping("/register/send-code")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<java.util.Map<String, Object>> sendCode(
            @org.springframework.web.bind.annotation.RequestParam String email,
            jakarta.servlet.http.HttpSession session) {
        synchronized (session) {
            long now = System.currentTimeMillis();
            Long start = (Long) session.getAttribute("emailSendWindow");
            Integer count = (Integer) session.getAttribute("emailSendCount");
            Long last = (Long) session.getAttribute("emailLastSent");
            if (last != null && now - last < 60_000) {
                return org.springframework.http.ResponseEntity.status(429).body(java.util.Map.of("message", "Vui lòng chờ 60 giây giữa các lần gửi mã."));
            }
            if (start == null || now - start >= 3_600_000) { start = now; count = 0; }
            if (count != null && count >= 10) {
                return org.springframework.http.ResponseEntity.status(429).body(java.util.Map.of("message", "Bạn đã gửi quá nhiều mã. Vui lòng thử lại sau một giờ."));
            }
            try {
                String error = emailVerification.sendCode(email, session.getId());
                if (error != null) return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("message", error));
                session.setAttribute("emailSendWindow", start);
                session.setAttribute("emailSendCount", count == null ? 1 : count + 1);
                session.setAttribute("emailLastSent", now);
                return org.springframework.http.ResponseEntity.ok(java.util.Map.of("message", "Đã gửi mã xác nhận. Kiểm tra hộp thư đến và thư rác.", "retryAfter", 60));
            } catch (org.springframework.mail.MailException | IllegalStateException | org.springframework.dao.DataIntegrityViolationException ex) {
                return org.springframework.http.ResponseEntity.status(503).body(java.util.Map.of("message", "Chưa gửi được email xác nhận. Vui lòng thử lại sau hoặc liên hệ hỗ trợ."));
            }
        }
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
        if (password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) return false;
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`]).{8,}$";
        return password.matches(regex);
    }
}
