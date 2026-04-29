package com.medsetu.service;

import com.medsetu.entity.*;
import com.medsetu.entity.ChatSession.SessionStatus;
import com.medsetu.exception.ResourceNotFoundException;
import com.medsetu.exception.ValidationException;
import com.medsetu.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AIChatService aiChatService;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       UserRepository userRepository,
                       AIChatService aiChatService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.aiChatService = aiChatService;
    }

    @Transactional
    public Map<String, Object> createSession(Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found."));

        ChatSession session = ChatSession.builder()
                .patient(patient)
                .status(SessionStatus.AI_HANDLING)
                .build();

        session = sessionRepository.save(session);
        return mapSession(session);
    }

    @Transactional
    public Map<String, Object> sendMessage(Long sessionId, Long patientId, String content) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found."));

        if (!session.getPatient().getId().equals(patientId)) {
            throw new ValidationException("Access denied.");
        }

        // Save patient message
        ChatMessage patientMessage = ChatMessage.builder()
                .session(session)
                .sender(ChatMessage.Sender.PATIENT)
                .content(content)
                .isEscalationTrigger(false)
                .build();
        messageRepository.save(patientMessage);

        // Get AI response
        String aiResponse = aiChatService.sendMessage(content);
        boolean needsEscalation = aiChatService.isEscalationRequired(aiResponse);

        // Save AI response
        ChatMessage aiMessage = ChatMessage.builder()
                .session(session)
                .sender(ChatMessage.Sender.AI)
                .content(aiResponse)
                .isEscalationTrigger(needsEscalation)
                .build();
        messageRepository.save(aiMessage);

        // Update session status if escalation needed
        if (needsEscalation && session.getStatus() == SessionStatus.AI_HANDLING) {
            session.setStatus(SessionStatus.ESCALATED);
            sessionRepository.save(session);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("sessionStatus", session.getStatus());
        result.put("aiResponse", aiResponse);
        result.put("isEscalated", needsEscalation);
        return result;
    }

    public List<Map<String, Object>> getChatHistory(Long sessionId, Long userId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found."));

        boolean isPatient = session.getPatient().getId().equals(userId);
        if (!isPatient) {
            // Allow doctors/admins to view escalated sessions
            User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
            if (user.getRole() == User.Role.PATIENT) {
                throw new ValidationException("Access denied.");
            }
        }

        return messageRepository.findBySessionIdOrderBySentAt(sessionId)
                .stream().map(this::mapMessage).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getEscalatedSessions() {
        return sessionRepository.findByStatusOrderByUpdatedAtDesc(SessionStatus.ESCALATED)
                .stream().map(this::mapSessionWithLastMessage).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> agentReply(Long sessionId, Long agentId, String content, boolean resolve) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat session not found."));

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found."));

        session.setAssignedAgent(agent);

        ChatMessage agentMessage = ChatMessage.builder()
                .session(session)
                .sender(ChatMessage.Sender.AGENT)
                .content(content)
                .isEscalationTrigger(false)
                .build();
        messageRepository.save(agentMessage);

        if (resolve) {
            session.setStatus(SessionStatus.RESOLVED);
        }

        sessionRepository.save(session);

        return mapSession(session);
    }

    private Map<String, Object> mapSession(ChatSession s) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", s.getId());
        result.put("status", s.getStatus());
        result.put("createdAt", s.getCreatedAt());
        result.put("updatedAt", s.getUpdatedAt());
        if (s.getPatient() != null) {
            result.put("patientId", s.getPatient().getId());
            result.put("patientName", s.getPatient().getName());
        }
        return result;
    }

    private Map<String, Object> mapSessionWithLastMessage(ChatSession s) {
        Map<String, Object> result = mapSession(s);
        ChatMessage last = messageRepository.findTopBySessionIdOrderBySentAtDesc(s.getId());
        if (last != null) {
            result.put("lastMessage", last.getContent().length() > 100
                    ? last.getContent().substring(0, 100) + "..." : last.getContent());
            result.put("lastMessageSender", last.getSender());
        }
        return result;
    }

    private Map<String, Object> mapMessage(ChatMessage m) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", m.getId());
        result.put("sender", m.getSender());
        result.put("content", m.getContent());
        result.put("isEscalationTrigger", m.getIsEscalationTrigger());
        result.put("sentAt", m.getSentAt());
        return result;
    }
}
