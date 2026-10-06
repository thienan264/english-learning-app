package com.project.englishlearning.controller;

import com.project.englishlearning.entity.CourseOrder;
import com.project.englishlearning.entity.PaymentTransaction;
import com.project.englishlearning.entity.UserCourseEnrollment;
import com.project.englishlearning.repository.CourseOrderRepository;
import com.project.englishlearning.repository.PaymentTransactionRepository;
import com.project.englishlearning.repository.UserCourseEnrollmentRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final CourseOrderRepository orderRepo;
    private final PaymentTransactionRepository transactionRepo;
    private final UserCourseEnrollmentRepository enrollmentRepo;
    private final com.project.englishlearning.repository.AdminNotificationRepository notificationRepo;

    public AdminOrderController(CourseOrderRepository orderRepo,
                                PaymentTransactionRepository transactionRepo,
                                UserCourseEnrollmentRepository enrollmentRepo,
                                com.project.englishlearning.repository.AdminNotificationRepository notificationRepo) {
        this.orderRepo = orderRepo;
        this.transactionRepo = transactionRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.notificationRepo = notificationRepo;
    }

    @GetMapping
    public String listOrders(Model model) {
        List<CourseOrder> orders = orderRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        model.addAttribute("orders", orders);
        return "admin/order-list";
    }

    @PostMapping("/{id}/manual-confirm")
    @Transactional
    public String manualConfirm(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CourseOrder order = orderRepo.findById(id).orElse(null);
        if (order != null && "PENDING".equals(order.getStatus())) {
            order.setStatus("PAID");
            orderRepo.save(order);

            // Create notification
            com.project.englishlearning.entity.AdminNotification notif = new com.project.englishlearning.entity.AdminNotification();
            notif.setTitle("Đơn hàng thủ công được xác nhận");
            notif.setMessage(order.getUser().getUsername() + " đã thanh toán thủ công cho khóa học " + order.getCourse().getTitle() + " số tiền " + order.getAmount() + "đ.");
            notif.setLink("/admin/orders");
            notificationRepo.save(notif);

            // Create manual transaction
            PaymentTransaction txn = new PaymentTransaction();
            txn.setOrder(order);
            txn.setTransactionNo("MANUAL-" + System.currentTimeMillis());
            txn.setBankCode("MANUAL");
            txn.setAmountPaid(order.getAmount());
            txn.setResponseCode("00");
            transactionRepo.save(txn);

            // Create enrollment if not exists
            UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(
                    order.getUser().getId(), order.getCourse().getId()).orElse(null);

            if (enrollment == null) {
                enrollment = new UserCourseEnrollment();
                enrollment.setUser(order.getUser());
                enrollment.setCourse(order.getCourse());
            }
            enrollment.setStatus("IN_PROGRESS");
            if (order.getCourse().getAccessDurationMonths() != null && order.getCourse().getAccessDurationMonths() > 0) {
                enrollment.setExpiresAt(java.time.LocalDateTime.now().plusMonths(order.getCourse().getAccessDurationMonths()));
            } else {
                enrollment.setExpiresAt(null);
            }
            enrollmentRepo.save(enrollment);
            
            redirectAttributes.addFlashAttribute("successMsg", "Đã xác nhận đơn hàng thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Đơn hàng không tồn tại hoặc đã được xử lý!");
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/revoke")
    @Transactional
    public String revokeOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CourseOrder order = orderRepo.findById(id).orElse(null);
        if (order != null && "PAID".equals(order.getStatus())) {
            order.setStatus("REFUNDED");
            orderRepo.save(order);

            // Revoke enrollment
            UserCourseEnrollment enrollment = enrollmentRepo.findByUserIdAndCourseId(
                    order.getUser().getId(), order.getCourse().getId()).orElse(null);

            if (enrollment != null) {
                enrollment.setStatus("REVOKED");
                enrollmentRepo.save(enrollment);
            }
            redirectAttributes.addFlashAttribute("successMsg", "Đã thu hồi quyền học thành công!");
        } else {
            redirectAttributes.addFlashAttribute("errorMsg", "Không thể thu hồi đơn hàng này!");
        }
        return "redirect:/admin/orders";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportOrders() throws IOException {
        List<CourseOrder> orders = orderRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Orders");
            
            // Header
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("Mã Đơn");
            headerRow.createCell(2).setCellValue("Người Dùng");
            headerRow.createCell(3).setCellValue("Khóa Học");
            headerRow.createCell(4).setCellValue("Số Tiền");
            headerRow.createCell(5).setCellValue("Trạng Thái");
            headerRow.createCell(6).setCellValue("Thời Gian");

            // Data
            int rowIdx = 1;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (CourseOrder order : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(order.getId());
                row.createCell(1).setCellValue(order.getOrderCode());
                row.createCell(2).setCellValue(order.getUser().getUsername());
                row.createCell(3).setCellValue(order.getCourse().getTitle());
                row.createCell(4).setCellValue(order.getAmount().doubleValue());
                row.createCell(5).setCellValue(order.getStatus());
                row.createCell(6).setCellValue(order.getCreatedAt().format(formatter));
            }

            workbook.write(out);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "orders.xlsx");
            
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        }
    }
}
