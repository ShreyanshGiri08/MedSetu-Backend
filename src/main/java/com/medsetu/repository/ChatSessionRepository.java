package com.medsetu.repository;

import com.medsetu.entity.ChatSession;
import com.medsetu.entity.ChatSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByPatientIdOrderByUpdatedAtDesc(Long patientId);
    List<ChatSession> findByStatusOrderByUpdatedAtDesc(SessionStatus status);
    long countByStatus(SessionStatus status);
}
