package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String hospitalName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> result = doctorService.listApprovedDoctors(specialization, hospitalName, page, size);
        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved.", result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorById(@PathVariable Long id) {
        Map<String, Object> doctor = doctorService.getDoctorById(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor retrieved.", doctor));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> doctor = doctorService.getMyProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved.", doctor));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> updates) {
        Map<String, Object> doctor = doctorService.updateDoctorProfile(currentUser.getId(), updates);
        return ResponseEntity.ok(ApiResponse.success("Doctor profile updated.", doctor));
    }

    @PostMapping("/me/profile-picture")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadProfilePicture(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {
        Map<String, Object> doctor = doctorService.uploadProfilePicture(currentUser.getId(), file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated.", doctor));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> approveDoctor(@PathVariable Long id) {
        doctorService.approveDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor approved successfully."));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> rejectDoctor(@PathVariable Long id) {
        doctorService.rejectDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor rejected and deactivated."));
    }
}
