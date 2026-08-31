package com.pulseim.message;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "message_delivery_events", indexes = @Index(name = "idx_delivery_message_time", columnList = "message_id, occurred_at"))
class DeliveryEventEntity {
    @Id private String id;
    @Column(name = "message_id", nullable = false, length = 80) private String messageId;
    @Column(nullable = false, length = 40) private String stage;
    @Column(length = 200) private String detail;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    protected DeliveryEventEntity() { }
    DeliveryEventEntity(String id, String messageId, String stage, String detail) {
        this.id = id; this.messageId = messageId; this.stage = stage; this.detail = detail; this.occurredAt = Instant.now();
    }
    String messageId() { return messageId; }
    String stage() { return stage; }
    String detail() { return detail; }
    Instant occurredAt() { return occurredAt; }
}