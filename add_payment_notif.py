import re

with open('src/main/java/com/project/englishlearning/service/PaymentService.java', 'r') as f:
    content = f.read()

# Add NotificationRepository to constructor
constructor_pattern = r'(private final VNPayConfig vnPayConfig;\s*private final com\.project\.englishlearning\.repository\.AdminNotificationRepository notificationRepo;\n)'
new_fields = r'\1    private final com.project.englishlearning.repository.NotificationRepository userNotificationRepo;\n'
content = re.sub(constructor_pattern, new_fields, content)

constructor_decl_pattern = r'(public PaymentService\(CourseOrderRepository orderRepo, PaymentTransactionRepository txRepo,\s*CourseRepository courseRepo, UserCourseEnrollmentRepository enrollmentRepo,\s*VNPayConfig vnPayConfig, com\.project\.englishlearning\.repository\.AdminNotificationRepository notificationRepo\)\s*\{)'
new_constructor_decl = r'''public PaymentService(CourseOrderRepository orderRepo, PaymentTransactionRepository txRepo,
                          CourseRepository courseRepo, UserCourseEnrollmentRepository enrollmentRepo,
                          VNPayConfig vnPayConfig, com.project.englishlearning.repository.AdminNotificationRepository notificationRepo,
                          com.project.englishlearning.repository.NotificationRepository userNotificationRepo) {'''
content = re.sub(constructor_decl_pattern, new_constructor_decl, content)

constructor_assign_pattern = r'(this\.notificationRepo = notificationRepo;\s*\})'
new_assign = r'this.notificationRepo = notificationRepo;\n        this.userNotificationRepo = userNotificationRepo;\n    }'
content = re.sub(constructor_assign_pattern, new_assign, content)

# Add User notification when PAID
paid_pattern = r'(notificationRepo\.save\(notif\);)'
user_notif = r'''notificationRepo.save(notif);
            
            // User Notification
            com.project.englishlearning.entity.Notification userNotif = new com.project.englishlearning.entity.Notification();
            userNotif.setUser(order.getUser());
            userNotif.setTitle("Thanh toán thành công!");
            userNotif.setMessage("Bạn đã thanh toán thành công khóa học " + order.getCourse().getTitle() + ". Bắt đầu học ngay thôi!");
            userNotif.setUrl("/learn/course/" + order.getCourse().getId());
            userNotificationRepo.save(userNotif);'''
content = re.sub(paid_pattern, user_notif, content)

with open('src/main/java/com/project/englishlearning/service/PaymentService.java', 'w') as f:
    f.write(content)

print("Updated PaymentService")
