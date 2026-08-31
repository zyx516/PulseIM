package com.pulseim.im;

import com.pulseim.common.mq.MessagePersistedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
class MessageEventListener {
    private final LocalConnectionRegistry connections;
    private final StringRedisTemplate redis;
    private final String nodeId;
    MessageEventListener(LocalConnectionRegistry connections, StringRedisTemplate redis, @Value("${pulseim.im.node-id:node-local}") String nodeId) { this.connections = connections; this.redis = redis; this.nodeId = nodeId; }
    @RabbitListener(queues = "#{imGatewayQueue.name}")
    void onMessagePersisted(MessagePersistedEvent event) {
        String key = "pulseim:mq:consumed:" + nodeId + ":" + event.eventId();
        Boolean firstDelivery = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(1));
        if (Boolean.TRUE.equals(firstDelivery)) connections.push(event, event.eventId());
    }
}