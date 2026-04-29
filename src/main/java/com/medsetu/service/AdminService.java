package com.medsetu.service;

import com.medsetu.entity.ChatSession.SessionStatus;
import com.medsetu.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    public AdminService(UserRepository userRepository,
                        DoctorRepository doctorRepository,
                        AppointmentRepository appointmentRepository,
                        PaymentRepository paymentRepository,
                        ChatSessionRepository chatSessionRepository,
                        ChatMessageRepository chatMessageRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalDoctors", doctorRepository.count());
        stats.put("appointmentsToday", appointmentRepository.countTodaysAppointments());
        stats.put("totalRevenue", paymentRepository.getTotalRevenue());
        stats.put("escalatedChats", chatSessionRepository.countByStatus(SessionStatus.ESCALATED));
        return stats;
    }

    public Map<String, Object> getAllUsers(int page, int size) {
        var pageable = PageRequest.of(page, size);
        var users = userRepository.findAll(pageable);
        Map<String, Object> result = new HashMap<>();
        result.put("content", users.getContent().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole());
            m.put("isActive", u.getIsActive());
            m.put("createdAt", u.getCreatedAt());
            return m;
        }).collect(Collectors.toList()));
        result.put("totalPages", users.getTotalPages());
        result.put("totalItems", users.getTotalElements());
        return result;
    }

    public List<Map<String, Object>> getEscalatedChatSessions() {
        return chatSessionRepository.findByStatusOrderByUpdatedAtDesc(SessionStatus.ESCALATED)
                .stream().map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("sessionId", s.getId());
                    m.put("patientName", s.getPatient().getName());
                    m.put("status", s.getStatus());
                    m.put("updatedAt", s.getUpdatedAt());
                    var last = chatMessageRepository.findTopBySessionIdOrderBySentAtDesc(s.getId());
                    if (last != null) {
                        String preview = last.getContent();
                        m.put("lastMessage", preview.length() > 100 ? preview.substring(0, 100) + "..." : preview);
                    }
                    return m;
                }).collect(Collectors.toList());
    }
}
