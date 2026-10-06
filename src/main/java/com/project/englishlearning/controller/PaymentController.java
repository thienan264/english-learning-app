package com.project.englishlearning.controller;

import com.project.englishlearning.entity.User;
import com.project.englishlearning.repository.UserRepository;
import com.project.englishlearning.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService, UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    @PostMapping("/checkout/{courseId}")
    public String checkout(@PathVariable Long courseId, Authentication auth, HttpServletRequest request) {
        User user = userRepository.findByUsername(auth.getName()).orElseThrow();
        String vnpayUrl = paymentService.createOrderAndGeneratePaymentUrl(user, courseId, request);
        return "redirect:" + vnpayUrl;
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request, Model model) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }
        
        // Process payment synchronously for localhost (fallback for IPN)
        paymentService.processIpn(fields);
        
        String responseCode = request.getParameter("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            model.addAttribute("message", "Thanh toán thành công! Khóa học đã được mở khóa.");
            model.addAttribute("status", "success");
        } else {
            model.addAttribute("message", "Thanh toán thất bại hoặc đã bị hủy.");
            model.addAttribute("status", "failed");
        }
        return "student/payment-result";
    }

    @GetMapping("/api/vnpay-ipn")
    @ResponseBody
    public ResponseEntity<String> vnpayIpn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                fields.put(fieldName, fieldValue);
            }
        }
        String response = paymentService.processIpn(fields);
        return ResponseEntity.ok(response);
    }
}
