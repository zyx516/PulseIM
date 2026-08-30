package com.pulseim.message;

import com.pulseim.common.mq.MessagePersistedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OutboxPublisher {
    private final OutboxEventRepository outboxEvents;
    private final RabbitTemplate rabbitTemplate;
    private final FanoutExchange exchange;
    private final ObjectMapper mapper;

    OutboxPublisher(OutboxEventRepository outboxEvents, RabbitTemplate rabbitTemplate, FanoutExchange exchange,
                    ObjectMapper mapper) {
        this.outboxEvents = outboxEvents;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.mapper = mapper;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    void retryPendingEvents() {
        outboxEvents.findTop50ByStatusOrderByCreatedAtAsc("PENDING").forEach(event -> {
            try {
                rabbitTemplate.convertAndSend(exchange.getName(), "", mapper.readValue(event.payload(), MessagePersistedEvent.class));
                event.markPublished();
            } catch (Exception ignored) {
            }
        });
    }
}
