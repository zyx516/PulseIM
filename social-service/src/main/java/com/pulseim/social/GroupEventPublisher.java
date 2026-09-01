package com.pulseim.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseim.common.mq.GroupMemberChangedEvent;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Configuration
class GroupEventConfig {
    @Bean
    FanoutExchange groupEventsExchange(@Value("${pulseim.group-events-exchange}") String name) {
        return new FanoutExchange(name, true, false);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }
}

@Component
class GroupEventPublisher {
    private final RabbitTemplate rabbit;
    private final FanoutExchange exchange;

    GroupEventPublisher(RabbitTemplate rabbit, FanoutExchange exchange, Jackson2JsonMessageConverter converter) {
        this.rabbit = rabbit;
        this.exchange = exchange;
        this.rabbit.setMessageConverter(converter);
    }

    void publish(String groupId, String userId, String action) {
        rabbit.convertAndSend(exchange.getName(), "", new GroupMemberChangedEvent("ge-" + UUID.randomUUID(), groupId, userId, action, Instant.now()));
    }
}