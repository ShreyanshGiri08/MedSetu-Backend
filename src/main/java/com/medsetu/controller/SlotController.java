package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.SlotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    private final SlotService slotService;

    public SlotController(SlotService slotService) {
        this.slotService = slotService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSlot(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        LocalDate date = LocalDate.parse(body.get("slotDate"));
        LocalTime startTime = LocalTime.parse(body.get("startTime"));
        LocalTime endTime = LocalTime.parse(body.get("endTime"));
        Map<String, Object> slot = slotService.createSlot(currentUser.getId(), date, startTime, endTime);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Slot created.", slot));
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam String date) {
        List<Map<String, Object>> slots = slotService.getAvailableSlots(doctorId, LocalDate.parse(date));
        return ResponseEntity.ok(ApiResponse.success("Available slots retrieved.", slots));
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDoctorSlots(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String date) {
        List<Map<String, Object>> slots = slotService.getMySlots(currentUser.getId(), LocalDate.parse(date));
        return ResponseEntity.ok(ApiResponse.success("Slots retrieved.", slots));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Void>> deleteSlot(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        slotService.deleteSlot(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Slot deleted."));
    }
}
