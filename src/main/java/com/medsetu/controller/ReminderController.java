package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.ReminderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {

    private final ReminderService reminderService;

    public ReminderController(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = reminderService.createReminder(
                currentUser.getId(),
                body.get("medicineName"),
                body.getOrDefault("dosage", null),
                body.get("timeOfDay"),
                body.getOrDefault("repeatDays", null));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reminder created.", result));
    }

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReminders(
            @AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> result = reminderService.getPatientReminders(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Reminders retrieved.", result));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = reminderService.toggleReminder(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Reminder toggled.", result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        reminderService.deleteReminder(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Reminder deleted."));
    }
}
