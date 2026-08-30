package com.pulseim.message;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "outbox_events", indexes = @Index(name = "idx_outbox_status", columnList = "status, created_at"))
class OutboxEventEntity {
    @Id
    private String id;
    @Column(nullable = false, length = 60)
    private String type;
    @Column(name = "aggregate_id", nullable = false, length = 80)
    private String aggregateId;
    @Lob
    @Column(nullable = false)
    private String payload;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEventEntity() {
    }

    OutboxEventEntity(String id, String type, String aggregateId, String payload, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = "PENDING";
        this.createdAt = createdAt;
    }

    String id() { return id; }
    String type() { return type; }
    String aggregateId() { return aggregateId; }
    String payload() { return payload; }
    String status() { return status; }
    Instant createdAt() { return createdAt; }
    Instant publishedAt() { return publishedAt; }

    void markPublished() {
        this.status = "PUBLISHED";
        this.publishedAt = Instant.now();
    }
}
