package com.project.englishlearning.service;

import com.project.englishlearning.config.VNPayConfig;
import com.project.englishlearning.entity.*;
import com.project.englishlearning.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class PaymentService {

    private final CourseOrderRepository orderRepo;
    private final PaymentTransactionRepository txRepo;
    private final CourseRepository courseRepo;
    private final UserCourseEnrollmentRepository enrollmentRepo;
    private final VNPayConfig vnPayConfig;
    private final com.project.englishlearning.repository.AdminNotificationRepository notificationRepo;
    private final com.project.englishlearning.repository.NotificationRepository userNotificationRepo;

    public PaymentService(CourseOrderRepository orderRepo, PaymentTransactionRepository txRepo,
                          CourseRepository courseRepo, UserCourseEnrollmentRepository enrollmentRepo,
                          VNPayConfig vnPayConfig, com.project.englishlearning.repository.AdminNotificationRepository notificationRepo,
                          com.project.englishlearning.repository.NotificationRepository userNotificationRepo) {
        this.orderRepo = orderRepo;
        this.txRepo = txRepo;
        this.courseRepo = courseRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.vnPayConfig = vnPayConfig;
        this.notificationRepo = notificationRepo;
        this.userNotificationRepo = userNotificationRepo;
    }

    @Transactional
    public String createOrderAndGeneratePaymentUrl(User user, Long courseId, HttpServletRequest request) {
        Course course = courseRepo.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        if (course.getIsFree() != null && course.getIsFree()) {
            throw new RuntimeException("This course is free!");
        }
        
        // Allow re-purchasing if expired
        UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(user.getId(), courseId).orElse(null);
        if (enrollment != null && enrollment.getExpiresAt() != null && java.time.LocalDateTime.now().isBefore(enrollment.getExpiresAt())) {
            throw new RuntimeException("You already own this course and it is not expired yet.");
        }

        String vnp_TxnRef = VNPayConfig.getRandomNumber(8);
        
        CourseOrder order = new CourseOrder();
        order.setOrderCode(vnp_TxnRef);
        order.setUser(user);
        order.setCourse(course);
        order.setAmount(course.getSalePrice() != null ? course.getSalePrice() : course.getPrice());
        order.setStatus("PENDING");
        orderRepo.save(order);

        // Build VNPay URL
        long amount = order.getAmount().longValue() * 100;
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan khoa hoc: " + course.getTitle());
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }
        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnPayConfig.vnp_HashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        return vnPayConfig.vnp_PayUrl + "?" + queryUrl;
    }

    @Transactional
    public String processIpn(Map<String, String> params) {
        String secureHash = params.get("vnp_SecureHash");
        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }
        params.remove("vnp_SecureHash");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        String signValue = VNPayConfig.hmacSHA512(vnPayConfig.vnp_HashSecret, hashData.toString());
        if (!signValue.equals(secureHash)) {
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid Checksum\"}";
        }

        String orderInfo = params.get("vnp_OrderInfo");
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_Amount = params.get("vnp_Amount");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");

        Optional<CourseOrder> optionalOrder = orderRepo.findByOrderCode(vnp_TxnRef);
        if (optionalOrder.isEmpty()) {
            return "{\"RspCode\":\"01\",\"Message\":\"Order not found\"}";
        }

        CourseOrder order = optionalOrder.get();
        long amount = order.getAmount().longValue() * 100;
        if (amount != Long.parseLong(vnp_Amount)) {
            return "{\"RspCode\":\"04\",\"Message\":\"Invalid amount\"}";
        }

        if (!"PENDING".equals(order.getStatus())) {
            return "{\"RspCode\":\"02\",\"Message\":\"Order already confirmed\"}";
        }

        // Create transaction record
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrder(order);
        tx.setTransactionNo(params.get("vnp_TransactionNo"));
        tx.setBankCode(params.get("vnp_BankCode"));
        tx.setAmountPaid(order.getAmount());
        tx.setResponseCode(vnp_ResponseCode);
        txRepo.save(tx);

        if ("00".equals(vnp_ResponseCode)) {
            order.setStatus("PAID");
            
            // Notification
            com.project.englishlearning.entity.AdminNotification notif = new com.project.englishlearning.entity.AdminNotification();
            notif.setTitle("Đơn hàng VNPay mới được thanh toán");
            notif.setMessage(order.getUser().getUsername() + " vừa thanh toán thành công khóa học " + order.getCourse().getTitle() + " qua VNPay. Số tiền: " + order.getAmount() + "đ.");
            notif.setLink("/admin/orders");
            notificationRepo.save(notif);
            
            // User Notification
            com.project.englishlearning.entity.Notification userNotif = new com.project.englishlearning.entity.Notification();
            userNotif.setUser(order.getUser());
            userNotif.setTitle("Thanh toán thành công!");
            userNotif.setMessage("Bạn đã thanh toán thành công khóa học " + order.getCourse().getTitle() + ". Bắt đầu học ngay thôi!");
            userNotif.setUrl("/courses/" + order.getCourse().getId());
            userNotificationRepo.save(userNotif);

            // Unlock course for user
            UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(order.getUser().getId(), order.getCourse().getId()).orElse(null);
            if (enrollment == null) {
                enrollment = new UserCourseEnrollment();
                enrollment.setUser(order.getUser());
                enrollment.setCourse(order.getCourse());
                enrollment.setCompletionPercentage(0.0);
            }
            enrollment.setStatus("ACTIVE");
            if (order.getCourse().getAccessDurationMonths() != null && order.getCourse().getAccessDurationMonths() > 0) {
                enrollment.setExpiresAt(java.time.LocalDateTime.now().plusMonths(order.getCourse().getAccessDurationMonths()));
            } else {
                enrollment.setExpiresAt(null); // Lifetime access if null or 0
            }
            enrollmentRepo.save(enrollment);
            
        } else {
            order.setStatus("FAILED");
        }
        orderRepo.save(order);
        return "{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}";
    }
}
