package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.service.AdminService;
import com.medsetu.service.DoctorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final DoctorService doctorService;

    public AdminController(AdminService adminService, DoctorService doctorService) {
        this.adminService = adminService;
        this.doctorService = doctorService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        Map<String, Object> result = adminService.getPlatformStats();
        return ResponseEntity.ok(ApiResponse.success("Platform stats retrieved.", result));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> result = adminService.getAllUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved.", result));
    }

    @PatchMapping("/doctors/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveDoctor(@PathVariable Long id) {
        doctorService.approveDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor approved."));
    }

    @PatchMapping("/doctors/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectDoctor(@PathVariable Long id) {
        doctorService.rejectDoctor(id);
        return ResponseEntity.ok(ApiResponse.success("Doctor rejected."));
    }

    @GetMapping("/chats/escalated")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEscalatedChats() {
        List<Map<String, Object>> result = adminService.getEscalatedChatSessions();
        return ResponseEntity.ok(ApiResponse.success("Escalated chats retrieved.", result));
    }
}
