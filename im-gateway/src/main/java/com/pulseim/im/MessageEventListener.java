package com.pulseim.im;

import com.pulseim.common.mq.MessagePersistedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
class MessageEventListener {
    private final LocalConnectionRegistry connections;

    MessageEventListener(LocalConnectionRegistry connections) {
        this.connections = connections;
    }

    @RabbitListener(queues = "#{imGatewayQueue.name}")
    void onMessagePersisted(MessagePersistedEvent event) {
        connections.push(event, event.eventId());
    }
}
