package com.pulseim.moderation;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "moderation_events", indexes = @Index(name = "idx_moderation_created", columnList = "created_at"))
class ModerationEventEntity {
    @Id
    private String id;
    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;
    @Column(name = "conversation_id", nullable = false, length = 80)
    private String conversationId;
    @Column(nullable = false, length = 20)
    private String result;
    @Column(length = 80)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ModerationEventEntity() {
    }

    ModerationEventEntity(String id, String userId, String conversationId, String result, String reason, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.conversationId = conversationId;
        this.result = result;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    String id() { return id; }
    String userId() { return userId; }
    String conversationId() { return conversationId; }
    String result() { return result; }
    String reason() { return reason; }
    Instant createdAt() { return createdAt; }
}
