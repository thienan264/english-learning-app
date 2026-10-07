import re

with open('src/main/java/com/project/englishlearning/controller/AdminOrderController.java', 'r') as f:
    content = f.read()

# Add NotificationRepository to constructor
constructor_pattern = r'(private final UserCourseEnrollmentRepository enrollmentRepo;\s*private final com\.project\.englishlearning\.repository\.AdminNotificationRepository notificationRepo;\n)'
new_fields = r'\1    private final com.project.englishlearning.repository.NotificationRepository userNotificationRepo;\n'
content = re.sub(constructor_pattern, new_fields, content)

constructor_decl_pattern = r'(public AdminOrderController\(CourseOrderRepository orderRepo,\s*PaymentTransactionRepository transactionRepo,\s*UserCourseEnrollmentRepository enrollmentRepo,\s*com\.project\.englishlearning\.repository\.AdminNotificationRepository notificationRepo\)\s*\{)'
new_constructor_decl = r'''public AdminOrderController(CourseOrderRepository orderRepo,
                                PaymentTransactionRepository transactionRepo,
                                UserCourseEnrollmentRepository enrollmentRepo,
                                com.project.englishlearning.repository.AdminNotificationRepository notificationRepo,
                                com.project.englishlearning.repository.NotificationRepository userNotificationRepo) {'''
content = re.sub(constructor_decl_pattern, new_constructor_decl, content)

constructor_assign_pattern = r'(this\.notificationRepo = notificationRepo;\s*\})'
new_assign = r'this.notificationRepo = notificationRepo;\n        this.userNotificationRepo = userNotificationRepo;\n    }'
content = re.sub(constructor_assign_pattern, new_assign, content)

# Add User notification when manually confirmed
paid_pattern = r'(notificationRepo\.save\(notif\);)'
user_notif = r'''notificationRepo.save(notif);

            // User Notification
            com.project.englishlearning.entity.Notification userNotif = new com.project.englishlearning.entity.Notification();
            userNotif.setUser(order.getUser());
            userNotif.setTitle("Đơn hàng được xác nhận!");
            userNotif.setMessage("Quản trị viên đã xác nhận thanh toán khóa học " + order.getCourse().getTitle() + ". Bắt đầu học ngay thôi!");
            userNotif.setUrl("/learn/course/" + order.getCourse().getId());
            userNotificationRepo.save(userNotif);'''
content = re.sub(paid_pattern, user_notif, content, count=1)

with open('src/main/java/com/project/englishlearning/controller/AdminOrderController.java', 'w') as f:
    f.write(content)

print("Updated AdminOrderController")
