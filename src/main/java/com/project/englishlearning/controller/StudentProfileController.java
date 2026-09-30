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
    private final com.project.englishlearning.repository.FlashcardTestResultRepository flashcardTestResultRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final com.project.englishlearning.repository.UserCourseEnrollmentRepository enrollmentRepo;
    private final org.modelmapper.ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;

    public StudentProfileController(UserRepository userRepository, 
                                    TestResultRepository testResultRepository, 
                                    com.project.englishlearning.repository.FlashcardTestResultRepository flashcardTestResultRepository,
                                    WritingSubmissionRepository writingSubmissionRepository,
                                    com.project.englishlearning.repository.UserCourseEnrollmentRepository enrollmentRepo,
                                    org.modelmapper.ModelMapper modelMapper,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.testResultRepository = testResultRepository;
        this.flashcardTestResultRepository = flashcardTestResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.enrollmentRepo = enrollmentRepo;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping
    public String viewProfile(@RequestParam(required = false) Long courseId, Authentication authentication, Model model) {
        try {
            String currentUsername = authentication.getName();
            User user = userRepository.findByUsername(currentUsername).orElseThrow();

            UserDTO safeUser = modelMapper.map(user, UserDTO.class);
            
            java.util.List<com.project.englishlearning.entity.UserCourseEnrollment> enrollments = enrollmentRepo.findByUserId(user.getId());
            java.util.List<com.project.englishlearning.entity.Course> myCourses = enrollments.stream()
                .map(com.project.englishlearning.entity.UserCourseEnrollment::getCourse)
                .collect(java.util.stream.Collectors.toList());

            java.util.List<com.project.englishlearning.entity.TestResult> tests = testResultRepository.findByUserIdOrderByCompletedAtDesc(user.getId());
            java.util.List<com.project.englishlearning.entity.WritingSubmission> writings = writingSubmissionRepository.findByUserIdOrderBySubmittedAtDesc(user.getId());
            java.util.List<com.project.englishlearning.entity.FlashcardTestResult> flashcardTests = flashcardTestResultRepository.findByUserIdOrderByCompletedAtDesc(user.getId());

            if (courseId != null) {
                tests = tests.stream()
                             .filter(t -> t.getLesson() != null && t.getLesson().getModule() != null && t.getLesson().getModule().getCourse() != null && t.getLesson().getModule().getCourse().getId().equals(courseId))
                             .collect(java.util.stream.Collectors.toList());
                writings = writings.stream()
                             .filter(w -> w.getLesson() != null && w.getLesson().getModule() != null && w.getLesson().getModule().getCourse() != null && w.getLesson().getModule().getCourse().getId().equals(courseId))
                             .collect(java.util.stream.Collectors.toList());
                flashcardTests = flashcardTests.stream()
                             .filter(ft -> ft.getCourse().getId().equals(courseId))
                             .collect(java.util.stream.Collectors.toList());
            }

            model.addAttribute("user", safeUser); 
            model.addAttribute("testResults", tests);
            model.addAttribute("writingSubmissions", writings);
            model.addAttribute("flashcardTests", flashcardTests);
            model.addAttribute("myCourses", myCourses);
            model.addAttribute("selectedCourseId", courseId);
            
            return "student/profile";
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
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