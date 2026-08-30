package com.pulseim.message;

import java.time.Instant;

public record MessageView(String id, String clientMessageId, String conversationId, String senderId, String toUserId,
                          String content, long sequence, String status, Instant createdAt, Instant recalledAt) {
    static MessageView from(MessageEntity entity) {
        return new MessageView(entity.id(), entity.clientMessageId(), entity.conversationId(), entity.senderId(),
                entity.toUserId(), entity.content(), entity.sequence(), entity.status(), entity.createdAt(), entity.recalledAt());
    }
}
