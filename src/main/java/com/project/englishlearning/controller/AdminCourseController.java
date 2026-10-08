package com.project.englishlearning.controller;

import com.project.englishlearning.entity.Course;
import com.project.englishlearning.service.CourseService;
import org.springframework.stereotype.Controller;
import com.project.englishlearning.repository.NotificationRepository;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.entity.Notification;
import java.util.List;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/admin/courses")
public class AdminCourseController {

    private final CourseService courseService;
    private final com.project.englishlearning.service.CourseReviewService reviewService;
    private final com.project.englishlearning.repository.CourseOrderRepository courseOrderRepository;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public AdminCourseController(CourseService courseService,
                                 com.project.englishlearning.repository.CourseOrderRepository courseOrderRepository,
                                 NotificationRepository notificationRepository,
                                 UserRepository userRepository, com.project.englishlearning.service.CourseReviewService reviewService) {
        this.courseService = courseService;
        this.reviewService = reviewService;
        this.courseOrderRepository = courseOrderRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    private String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        try {
            String uploadDir = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) dir.mkdirs();
            
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.-]", "_");
            Path path = Paths.get(uploadDir + filename);
            Files.write(path, file.getBytes());
            return "/uploads/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        
        // Purchase count map: courseId -> count
        java.util.List<Object[]> purchaseCounts = courseOrderRepository.countPurchasesByCourse();
        java.util.Map<Long, Long> purchaseCountMap = new java.util.HashMap<>();
        for (Object[] row : purchaseCounts) {
            purchaseCountMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        model.addAttribute("purchaseCountMap", purchaseCountMap);
        model.addAttribute("reviewStats", reviewService.statistics());
        
        return "admin/course-list"; 
    }

    @PostMapping("/add")
    public String addCourse(@ModelAttribute Course course, 
                            @RequestParam(value = "isFree", required = false) Boolean isFree,
                            @RequestParam(value = "thumbnailImage", required = false) MultipartFile image) {
        
        course.setIsFree(isFree != null ? isFree : false);
        
        com.project.englishlearning.service.CourseDataRules.validate(course);
        course.setId(null);
        String imageUrl = saveImage(image);
        if (imageUrl != null) {
            course.setThumbnailUrl(imageUrl);
        }
        
        courseService.saveCourse(course);
        
        // Notify all users about the new course
        List<com.project.englishlearning.entity.User> allUsers = userRepository.findAll();
        for (com.project.englishlearning.entity.User u : allUsers) {
            Notification n = new Notification();
            n.setUser(u);
            n.setTitle("Khóa học mới: " + course.getTitle());
            n.setMessage("Hệ thống vừa ra mắt khóa học mới: " + course.getTitle() + ". Hãy khám phá ngay!");
            n.setUrl("/courses/" + course.getId());
            notificationRepository.save(n);
        }

        return "redirect:/admin/courses"; 
    }

    @PostMapping("/edit/{id}")
    public String editCourse(@PathVariable Long id, 
                             @ModelAttribute Course updatedCourse, 
                             @RequestParam(value = "isFree", required = false) Boolean isFree,
                             @RequestParam(value = "thumbnailImage", required = false) MultipartFile image) {
        updatedCourse.setIsFree(isFree != null ? isFree : false);
        com.project.englishlearning.service.CourseDataRules.validate(updatedCourse);
        Course existingCourse = courseService.getCourseById(id);
        existingCourse.setTitle(updatedCourse.getTitle());
        existingCourse.setLevel(updatedCourse.getLevel());
        existingCourse.setDescription(updatedCourse.getDescription());
        
        String imageUrl = saveImage(image);
        if (imageUrl != null) {
            existingCourse.setThumbnailUrl(imageUrl);
        }
        
        // Update pricing
        existingCourse.setIsFree(isFree != null ? isFree : false);
        existingCourse.setPrice(updatedCourse.getPrice());
        existingCourse.setSalePrice(updatedCourse.getSalePrice());
        existingCourse.setSaleStartDate(updatedCourse.getSaleStartDate());
        existingCourse.setSaleEndDate(updatedCourse.getSaleEndDate());
        existingCourse.setAccessDurationMonths(updatedCourse.getAccessDurationMonths());
        
        courseService.saveCourse(existingCourse);
        return "redirect:/admin/courses";
    }

    @PostMapping("/delete/{id}")
    public String deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return "redirect:/admin/courses";
    }
}
