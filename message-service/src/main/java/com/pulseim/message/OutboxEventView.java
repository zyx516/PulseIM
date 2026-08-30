package com.pulseim.message;

import java.time.Instant;

public record OutboxEventView(String id, String type, String aggregateId, String status, Instant createdAt,
                              Instant publishedAt) {
    static OutboxEventView from(OutboxEventEntity entity) {
        return new OutboxEventView(entity.id(), entity.type(), entity.aggregateId(), entity.status(),
                entity.createdAt(), entity.publishedAt());
    }
}
