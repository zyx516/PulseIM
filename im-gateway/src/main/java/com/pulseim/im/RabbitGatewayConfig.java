package com.pulseim.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RabbitGatewayConfig {
    @Bean
    FanoutExchange messageEventsExchange(@Value("${pulseim.rabbitmq.exchange:pulseim.message.events}") String exchangeName) {
        return new FanoutExchange(exchangeName, true, false);
    }

    @Bean
    Queue imGatewayQueue(@Value("${pulseim.im.node-id:node-local}") String nodeId) {
        return QueueBuilder.durable("pulseim.im." + nodeId + ".message-events").build();
    }

    @Bean
    Binding imGatewayBinding(FanoutExchange messageEventsExchange, Queue imGatewayQueue) {
        return BindingBuilder.bind(imGatewayQueue).to(messageEventsExchange);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                       Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setMissingQueuesFatal(false);
        return factory;
    }
}
