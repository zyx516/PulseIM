package com.pulseim.conversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class GroupEventRabbitConfig {
    @Bean
    FanoutExchange groupEventsExchange() {
        return new FanoutExchange("pulseim.group.events", true, false);
    }

    @Bean
    Queue groupMemberQueue() {
        return QueueBuilder.durable("pulseim.conversation.group-members").build();
    }

    @Bean
    Binding groupMemberBinding(FanoutExchange groupEventsExchange, Queue groupMemberQueue) {
        return BindingBuilder.bind(groupMemberQueue).to(groupEventsExchange);
    }

    @Bean
    Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(converter);
        factory.setMissingQueuesFatal(false);
        return factory;
    }
}