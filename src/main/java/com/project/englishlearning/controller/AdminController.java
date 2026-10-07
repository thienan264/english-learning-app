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
    private final SiteActivityRepository activity;
    private final com.project.englishlearning.service.CourseReviewService reviewService;

    public AdminController(UserRepository userRepository, CourseRepository courseRepository,
                           LessonRepository lessonRepository, TestResultRepository testResultRepository,
                           WritingSubmissionRepository writingSubmissionRepository,
                           CourseOrderRepository courseOrderRepository,
                           UserCourseEnrollmentRepository enrollmentRepo,
                           PaymentTransactionRepository txRepo, com.project.englishlearning.service.CourseReviewService reviewService, SiteActivityRepository activity) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.testResultRepository = testResultRepository;
        this.writingSubmissionRepository = writingSubmissionRepository;
        this.courseOrderRepository = courseOrderRepository;
        this.enrollmentRepo = enrollmentRepo;
        this.txRepo = txRepo;
        this.reviewService = reviewService;
        this.activity=activity;
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
        var today=java.time.LocalDate.now();
        var yearStart=today.withDayOfYear(1).atStartOfDay();
        var decemberRevenue=courseOrderRepository.revenueInPeriod(yearStart.minusMonths(1),yearStart);
        model.addAttribute("previousDecemberRevenue", decemberRevenue != null ? decemberRevenue : BigDecimal.ZERO);
        model.addAttribute("currentMonth", today.getMonthValue());
        model.addAttribute("monthlyRegistrations", monthlyValues(userRepository.monthlyRegistrations(),1,1));
        model.addAttribute("monthlyPurchases", monthlyValues(courseOrderRepository.monthlyPurchases(),1,1));
        var activityRows=activity.monthlyActivity();
        model.addAttribute("monthlyVisits", monthlyValues(activityRows,1,1));
        model.addAttribute("monthlyStudyHours", monthlyValues(activityRows,2,3600));
        model.addAttribute("trackingStartedAt", activity.trackingStartedAt());
        model.addAttribute("dashboardNavigation", true);

        List<Object[]> topCourses = courseOrderRepository.getTopSellingCourses();
        model.addAttribute("topCourses", topCourses);
        model.addAttribute("courseStats", reviewService.statistics());

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

    private List<Double> monthlyValues(List<Object[]> rows,int column,double divisor) {
        List<Double> result=new ArrayList<>(java.util.Collections.nCopies(12,0.0));
        for(var row:rows) result.set(((Number)row[0]).intValue()-1,((Number)row[column]).doubleValue()/divisor);
        return result;
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(defaultValue="newest") String sort, Model model) {
        boolean oldest = "oldest".equals(sort);
        var direction = oldest ? org.springframework.data.domain.Sort.Direction.ASC : org.springframework.data.domain.Sort.Direction.DESC;
        List<User> users = userRepository.findAll(org.springframework.data.domain.Sort.by(direction, "createdAt", "id"));
        model.addAttribute("accountSort", oldest ? "oldest" : "newest");
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