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
    private final com.project.englishlearning.repository.CourseOrderRepository orderRepository;
    private final com.project.englishlearning.repository.UserLessonProgressRepository progressRepository;

    public StudentProfileController(UserRepository userRepository, 
                                    TestResultRepository testResultRepository, 
                                    com.project.englishlearning.repository.FlashcardTestResultRepository flashcardTestResultRepository,
                                    WritingSubmissionRepository writingSubmissionRepository,
                                    com.project.englishlearning.repository.UserCourseEnrollmentRepository enrollmentRepo,
                                    org.modelmapper.ModelMapper modelMapper,
                                    PasswordEncoder passwordEncoder,
                                    com.project.englishlearning.repository.CourseOrderRepository orderRepository,
                                    com.project.englishlearning.repository.UserLessonProgressRepository progressRepository) {
        this.userRepository = userRepository;
        this.testResultRepository = testResultRepository;
        this.flashcardTestResultRepository = flashcardTestResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.enrollmentRepo = enrollmentRepo;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.orderRepository = orderRepository;
        this.progressRepository = progressRepository;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    @GetMapping("/learning-history")
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
            
            var progress = progressRepository.findByUserId(user.getId()).stream()
                .filter(p -> p.getLastAccessedAt() != null || p.getCompletedAt() != null)
                .filter(p -> courseId == null || (p.getLesson().getModule() != null && p.getLesson().getModule().getCourse().getId().equals(courseId)))
                .sorted(java.util.Comparator.comparing(com.project.englishlearning.entity.UserLessonProgress::getLastAccessedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .toList();
            model.addAttribute("lessonProgress", progress);
            return "student/learning-history";
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping
    public String account(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("user", modelMapper.map(user, UserDTO.class));
        return "student/profile";
    }

    @GetMapping("/payments")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public String payments(Authentication authentication, Model model) {
        User user = userRepository.findByUsername(authentication.getName()).orElseThrow();
        model.addAttribute("orders", orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        return "student/payment-history";
    }

    @PostMapping("/update")
    public String updateProfile(@RequestParam("fullName") String fullName, 
                                @RequestParam("email") String email,
                                @RequestParam(defaultValue = "") String phone,
                                @RequestParam(defaultValue = "") String dateOfBirth,
                                @RequestParam(defaultValue = "") String city,
                                @RequestParam(defaultValue = "") String learningGoal,
                                @RequestParam(required = false) org.springframework.web.multipart.MultipartFile avatar,
                                @RequestParam(defaultValue = "false") boolean removeAvatar,
                                Authentication authentication, 
                                RedirectAttributes redirectAttributes) {
        String currentUsername = authentication.getName();
        User user = userRepository.findByUsername(currentUsername).orElseThrow();

        fullName = fullName.trim();
        email = email.trim();
        if (!user.getEmail().equals(email) && userRepository.existsByEmail(email)) {
            redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng bởi người dùng khác.");
            return "redirect:/profile";
        }

        java.time.LocalDate birthday;
        try {
            birthday = dateOfBirth.isBlank() ? null : java.time.LocalDate.parse(dateOfBirth);
            if (birthday != null && birthday.isAfter(java.time.LocalDate.now())) throw new IllegalArgumentException();
            if (fullName.isBlank() || fullName.length() > 100 || email.length() > 100 ||
                !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") ||
                (!phone.isBlank() && !phone.matches("[+0-9() .-]{8,20}")) || city.length() > 100 || learningGoal.length() > 250)
                throw new IllegalArgumentException();
        } catch (IllegalArgumentException | java.time.format.DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("error", "Thông tin chưa hợp lệ. Kiểm tra email, số điện thoại và ngày sinh.");
            return "redirect:/profile";
        }
        if (avatar != null && !avatar.isEmpty()) {
            try {
                if (avatar.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException();
                var image = javax.imageio.ImageIO.read(avatar.getInputStream());
                if (image == null || image.getWidth() > 4096 || image.getHeight() > 4096) throw new IllegalArgumentException();
                var directory = java.nio.file.Path.of("uploads", "avatars");
                java.nio.file.Files.createDirectories(directory);
                String filename = java.util.UUID.randomUUID() + ".png";
                javax.imageio.ImageIO.write(image, "png", directory.resolve(filename).toFile());
                user.setAvatarUrl("/uploads/avatars/" + filename);
            } catch (java.io.IOException | IllegalArgumentException ex) {
                redirectAttributes.addFlashAttribute("error", "Ảnh không hợp lệ. Chọn ảnh JPG hoặc PNG tối đa 5 MB, kích thước tối đa 4096 × 4096.");
                return "redirect:/profile";
            }
        } else if (removeAvatar) {
            user.setAvatarUrl(null);
        }
        user.setPhone(phone.trim());
        user.setDateOfBirth(birthday);
        user.setCity(city.trim());
        user.setLearningGoal(learningGoal.trim());
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
