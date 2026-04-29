package com.medsetu.controller;

import com.medsetu.dto.ApiResponse;
import com.medsetu.entity.User;
import com.medsetu.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createSession(
            @AuthenticationPrincipal User currentUser) {
        Map<String, Object> result = chatService.createSession(currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Chat session created.", result));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        Map<String, Object> result = chatService.sendMessage(sessionId, currentUser.getId(), body.get("content"));
        return ResponseEntity.ok(ApiResponse.success("Message sent.", result));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChatHistory(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser) {
        List<Map<String, Object>> result = chatService.getChatHistory(sessionId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Chat history retrieved.", result));
    }

    @GetMapping("/escalated")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getEscalated() {
        List<Map<String, Object>> result = chatService.getEscalatedSessions();
        return ResponseEntity.ok(ApiResponse.success("Escalated sessions retrieved.", result));
    }

    @PostMapping("/sessions/{sessionId}/agent-reply")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> agentReply(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        boolean resolve = Boolean.TRUE.equals(body.get("resolve"));
        Map<String, Object> result = chatService.agentReply(sessionId, currentUser.getId(), content, resolve);
        return ResponseEntity.ok(ApiResponse.success("Reply sent.", result));
    }
}
