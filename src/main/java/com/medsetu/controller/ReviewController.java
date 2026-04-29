package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitReview(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        Long doctorId = Long.parseLong(body.get("doctorId").toString());
        Long appointmentId = body.containsKey("appointmentId") && body.get("appointmentId") != null
                ? Long.parseLong(body.get("appointmentId").toString()) : null;
        int rating = Integer.parseInt(body.get("rating").toString());
        String comment = (String) body.getOrDefault("comment", null);

        Map<String, Object> result = reviewService.submitReview(
                currentUser.getId(), doctorId, appointmentId, rating, comment);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted.", result));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDoctorReviews(
            @PathVariable Long doctorId) {
        List<Map<String, Object>> result = reviewService.getDoctorReviews(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved.", result));
    }
}
