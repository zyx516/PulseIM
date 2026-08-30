package com.pulseim.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface MessageRepository extends JpaRepository<MessageEntity, String> {
    Optional<MessageEntity> findBySenderIdAndClientMessageId(String senderId, String clientMessageId);
    List<MessageEntity> findTop50ByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(String conversationId, long sequence);
}
