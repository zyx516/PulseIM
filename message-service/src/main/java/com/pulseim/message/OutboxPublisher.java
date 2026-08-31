package com.pulseim.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseim.common.mq.MessagePersistedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxPublisher {
    private final OutboxEventRepository outboxEvents; private final RabbitTemplate rabbitTemplate; private final org.springframework.amqp.core.FanoutExchange exchange; private final ObjectMapper mapper; private final MessageService messageService; private final int maxAttempts;
    OutboxPublisher(OutboxEventRepository outboxEvents, RabbitTemplate rabbitTemplate, org.springframework.amqp.core.FanoutExchange exchange, ObjectMapper mapper, MessageService messageService, @Value("${pulseim.outbox.max-attempts:5}") int maxAttempts) { this.outboxEvents = outboxEvents; this.rabbitTemplate = rabbitTemplate; this.exchange = exchange; this.mapper = mapper; this.messageService = messageService; this.maxAttempts = maxAttempts; }
    @Scheduled(fixedDelayString = "${pulseim.outbox.retry-delay-ms:3000}") @Transactional void retryPendingEvents() { outboxEvents.findTop50ByStatusOrderByCreatedAtAsc("PENDING").forEach(event -> { try { rabbitTemplate.convertAndSend(exchange.getName(), "", mapper.readValue(event.payload(), MessagePersistedEvent.class)); event.markPublished(); messageService.recordPublished(event.aggregateId()); } catch (Exception exception) { event.recordFailure(exception.getMessage(), maxAttempts); messageService.recordPublishFailure(event.aggregateId(), "RabbitMQ publication failed: " + event.lastError(), "DEAD".equals(event.status())); } }); }
}