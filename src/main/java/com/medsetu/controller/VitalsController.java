package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.VitalsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vitals")
public class VitalsController {

    private final VitalsService vitalsService;

    public VitalsController(VitalsService vitalsService) {
        this.vitalsService = vitalsService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> log(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> result = vitalsService.logVitals(currentUser.getId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vitals logged.", result));
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAll(
            @AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> result = vitalsService.getAllVitals(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Vitals retrieved.", result));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTrend(
            @AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> result = vitalsService.getVitalsLast30Days(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Vitals trend retrieved.", result));
    }
}
