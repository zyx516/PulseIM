package com.pulseim.message;

import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
class RabbitMessageConfig {
    @Bean
    FanoutExchange messageEventsExchange(@Value("${pulseim.rabbitmq.exchange:pulseim.message.events}") String exchangeName) {
        return new FanoutExchange(exchangeName, true, false);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }
}
