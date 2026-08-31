package com.pulseim.message;

import java.time.Instant;

public record DeliveryEventView(String stage, String detail, Instant occurredAt) {
    static DeliveryEventView from(DeliveryEventEntity entity) {
        return new DeliveryEventView(entity.stage(), entity.detail(), entity.occurredAt());
    }
}