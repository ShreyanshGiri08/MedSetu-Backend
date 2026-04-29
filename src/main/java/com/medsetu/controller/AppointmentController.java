package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> book(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        Long doctorId = Long.parseLong(body.get("doctorId").toString());
        Long slotId = body.containsKey("slotId") && body.get("slotId") != null
                ? Long.parseLong(body.get("slotId").toString()) : null;
        String type = (String) body.getOrDefault("type", "OFFLINE");
        String notes = (String) body.getOrDefault("notes", null);

        Map<String, Object> result = appointmentService.bookAppointment(
                currentUser.getId(), doctorId, slotId, type, notes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment booked.", result));
    }

    @GetMapping("/patient")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPatientAppointments(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String status) {
        List<Map<String, Object>> result = appointmentService.getPatientAppointments(currentUser.getId(), status);
        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved.", result));
    }

    @GetMapping("/doctor")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDoctorAppointments(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String date) {
        List<Map<String, Object>> result = appointmentService.getDoctorAppointments(currentUser.getId(), date);
        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved.", result));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = appointmentService.cancelAppointment(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled.", result));
    }

    @PatchMapping("/{id}/reschedule")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reschedule(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        Long newSlotId = Long.parseLong(body.get("newSlotId").toString());
        Map<String, Object> result = appointmentService.rescheduleAppointment(id, newSlotId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Appointment rescheduled.", result));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = appointmentService.updateAppointmentStatus(id, body.get("status"), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Appointment status updated.", result));
    }
}
