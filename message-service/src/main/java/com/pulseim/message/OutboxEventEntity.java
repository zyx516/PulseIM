package com.pulseim.message;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_status", columnList = "status, created_at"))
class OutboxEventEntity {
    @Id private String id;
    @Column(nullable = false, length = 60) private String type;
    @Column(name = "aggregate_id", nullable = false, length = 80) private String aggregateId;
    @Lob @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String payload;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", length = 1000) private String lastError;
    protected OutboxEventEntity() { }
    OutboxEventEntity(String id, String type, String aggregateId, String payload, Instant createdAt) { this.id = id; this.type = type; this.aggregateId = aggregateId; this.payload = payload; this.status = "PENDING"; this.createdAt = createdAt; }
    String id() { return id; } String type() { return type; } String aggregateId() { return aggregateId; } String payload() { return payload; } String status() { return status; } Instant createdAt() { return createdAt; } Instant publishedAt() { return publishedAt; } int attempts() { return attempts; } String lastError() { return lastError; }
    void markPublished() { this.status = "PUBLISHED"; this.publishedAt = Instant.now(); this.lastError = null; }
    void recordFailure(String message, int maxAttempts) { this.attempts++; this.lastError = message == null ? "Unknown publishing error" : message.substring(0, Math.min(1000, message.length())); if (this.attempts >= maxAttempts) this.status = "DEAD"; }
    void retry() { this.status = "PENDING"; this.attempts = 0; this.lastError = null; }
}