package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> profile = userService.getProfile(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved.", profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> updates) {
        Map<String, Object> profile = userService.updateProfile(currentUser.getId(), updates);
        return ResponseEntity.ok(ApiResponse.success("Profile updated.", profile));
    }
}
