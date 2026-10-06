package com.project.englishlearning.controller;

import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final TestResultRepository testResultRepository;
    private final WritingSubmissionRepository writingSubmissionRepository;
    private final CourseOrderRepository courseOrderRepository;
    private final UserCourseEnrollmentRepository enrollmentRepo;
    private final PaymentTransactionRepository txRepo;

    public AdminController(UserRepository userRepository, CourseRepository courseRepository,
                           LessonRepository lessonRepository, TestResultRepository testResultRepository,
                           WritingSubmissionRepository writingSubmissionRepository,
                           CourseOrderRepository courseOrderRepository,
                           UserCourseEnrollmentRepository enrollmentRepo,
                           PaymentTransactionRepository txRepo) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.testResultRepository = testResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.courseOrderRepository = courseOrderRepository;
        this.enrollmentRepo = enrollmentRepo;
        this.txRepo = txRepo;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalCourses", courseRepository.count());
        model.addAttribute("totalLessons", lessonRepository.count());
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalTests", testResultRepository.count());
        model.addAttribute("totalWritings", writingSubmissionRepository.count());
        
        BigDecimal totalRevenue = courseOrderRepository.calculateTotalRevenue();
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        List<Object[]> monthlyRevData = courseOrderRepository.getMonthlyRevenueCurrentYear();
        List<BigDecimal> monthlyRevenue = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            BigDecimal rev = BigDecimal.ZERO;
            for (Object[] row : monthlyRevData) {
                if (((Number) row[0]).intValue() == i) {
                    rev = (BigDecimal) row[1];
                    break;
                }
            }
            monthlyRevenue.add(rev);
        }
        model.addAttribute("monthlyRevenue", monthlyRevenue);

        List<Object[]> topCourses = courseOrderRepository.getTopSellingCourses();
        model.addAttribute("topCourses", topCourses);

        // Courses on sale with purchase counts
        List<com.project.englishlearning.entity.Course> saleCourses = courseRepository.findCoursesWithSalePrice();
        model.addAttribute("saleCourses", saleCourses);

        // Purchase count map: courseId -> count
        List<Object[]> purchaseCounts = courseOrderRepository.countPurchasesByCourse();
        java.util.Map<Long, Long> purchaseCountMap = new java.util.HashMap<>();
        for (Object[] row : purchaseCounts) {
            purchaseCountMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        model.addAttribute("purchaseCountMap", purchaseCountMap);

        // Recent transactions (last 20 orders)
        List<com.project.englishlearning.entity.CourseOrder> recentOrders = courseOrderRepository.findAllByOrderByCreatedAtDesc();
        if (recentOrders.size() > 20) {
            recentOrders = recentOrders.subList(0, 20);
        }
        model.addAttribute("recentOrders", recentOrders);

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/user-list";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleUserStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElseThrow();
        if ("ACTIVE".equals(user.getStatus())) {
            user.setStatus("LOCKED");
        } else {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái tài khoản thành công.");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        model.addAttribute("user", user);
        
        List<com.project.englishlearning.entity.UserCourseEnrollment> enrollments = enrollmentRepo.findByUserId(id);
        model.addAttribute("enrollments", enrollments);
        
        List<com.project.englishlearning.entity.CourseOrder> orders = courseOrderRepository.findByUserIdOrderByCreatedAtDesc(id);
        model.addAttribute("orders", orders);
        
        List<com.project.englishlearning.entity.TestResult> tests = testResultRepository.findByUserIdOrderByCompletedAtDesc(id);
        model.addAttribute("tests", tests);
        
        return "admin/user-details";
    }
}