package com.pulseim.common.mq;

import java.time.Instant;

public record MessagePersistedEvent(String eventId, String messageId, String clientMessageId, String conversationId,
                                    String senderId, String toUserId, String content, long sequence,
                                    Instant createdAt, Instant eventCreatedAt) {
}
