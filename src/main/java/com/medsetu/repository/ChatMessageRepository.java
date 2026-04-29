package com.medsetu.repository;

import com.medsetu.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderBySentAt(Long sessionId);

    // get the last message in a session
    ChatMessage findTopBySessionIdOrderBySentAtDesc(Long sessionId);
}
