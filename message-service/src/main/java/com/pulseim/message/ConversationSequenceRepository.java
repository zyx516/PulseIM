package com.pulseim.message;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

interface ConversationSequenceRepository extends JpaRepository<ConversationSequenceEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConversationSequenceEntity> findByConversationId(String conversationId);
}
