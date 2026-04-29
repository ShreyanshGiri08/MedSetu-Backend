package com.medsetu.service;

import com.medsetu.entity.Payment;
import com.medsetu.entity.Payment.PaymentStatus;
import com.medsetu.entity.User;
import com.medsetu.exception.PaymentFailedException;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.repository.PaymentRepository;
import com.medsetu.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final String razorpayKeyId;
    private final String razorpayKeySecret;
    private final String currency;

    public PaymentService(PaymentRepository paymentRepository,
                          UserRepository userRepository,
                          RestTemplate restTemplate,
                          @Value("${razorpay.key.id}") String razorpayKeyId,
                          @Value("${razorpay.key.secret}") String razorpayKeySecret,
                          @Value("${razorpay.currency}") String currency) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
        this.razorpayKeyId = razorpayKeyId;
        this.razorpayKeySecret = razorpayKeySecret;
        this.currency = currency;
    }

    @Transactional
    public Map<String, Object> createOrder(Long patientId, BigDecimal amount) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));

        // Call Razorpay Orders API
        String url = "https://api.razorpay.com/v1/orders";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // convert to paise
        requestBody.put("currency", currency);
        requestBody.put("receipt", "order_" + System.currentTimeMillis());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> razorpayResponse = restTemplate.postForObject(url, entity, Map.class);

        String razorpayOrderId = razorpayResponse != null ? (String) razorpayResponse.get("id") : null;

        Payment payment = Payment.builder()
                .patient(patient)
                .amount(amount)
                .razorpayOrderId(razorpayOrderId)
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", payment.getId());
        result.put("razorpayOrderId", razorpayOrderId);
        result.put("amount", amount);
        result.put("currency", currency);
        result.put("keyId", razorpayKeyId);
        return result;
    }

    @Transactional
    public Map<String, Object> verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found."));

        String signatureData = razorpayOrderId + "|" + razorpayPaymentId;
        boolean isValid = verifyHmacSha256(signatureData, razorpaySignature);

        if (isValid) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionId(razorpayPaymentId);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentFailedException("Payment signature verification failed.");
        }

        payment = paymentRepository.save(payment);

        Map<String, Object> result = new HashMap<>();
        result.put("paymentId", payment.getId());
        result.put("status", payment.getStatus());
        result.put("transactionId", payment.getTransactionId());
        return result;
    }

    public List<Map<String, Object>> getPaymentHistory(Long patientId) {
        return paymentRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream().map(p -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", p.getId());
                    m.put("amount", p.getAmount());
                    m.put("status", p.getStatus());
                    m.put("razorpayOrderId", p.getRazorpayOrderId());
                    m.put("transactionId", p.getTransactionId());
                    m.put("createdAt", p.getCreatedAt());
                    return m;
                }).collect(java.util.stream.Collectors.toList());
    }

    private boolean verifyHmacSha256(String data, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(razorpayKeySecret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());
            String computedSignature = bytesToHex(hash);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
