package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        Map<String, Object> result = paymentService.createOrder(currentUser.getId(), amount);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Razorpay order created.", result));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verify(
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = paymentService.verifyPayment(
                body.get("razorpayOrderId"),
                body.get("razorpayPaymentId"),
                body.get("razorpaySignature"));
        return ResponseEntity.ok(ApiResponse.success("Payment verified.", result));
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHistory(
            @AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> result = paymentService.getPaymentHistory(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Payment history retrieved.", result));
    }
}
