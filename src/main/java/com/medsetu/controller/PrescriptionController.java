package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.PrescriptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        Long patientId = Long.parseLong(body.get("patientId").toString());
        Long appointmentId = body.containsKey("appointmentId") && body.get("appointmentId") != null
                ? Long.parseLong(body.get("appointmentId").toString()) : null;
        String notes = (String) body.getOrDefault("notes", null);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> medicines = (List<Map<String, String>>) body.get("medicines");

        Map<String, Object> result = prescriptionService.createPrescription(
                currentUser.getId(), patientId, appointmentId, notes, medicines);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Prescription created.", result));
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyPrescriptions(
            @AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> result = prescriptionService.getPatientPrescriptions(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Prescriptions retrieved.", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrescriptionDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = prescriptionService.getPrescriptionDetail(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Prescription retrieved.", result));
    }
}
