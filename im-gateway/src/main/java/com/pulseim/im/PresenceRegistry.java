package com.pulseim.im;

import com.pulseim.common.security.JwtSupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
class PresenceRegistry {
    private final StringRedisTemplate redis;
    private final String nodeId;

    PresenceRegistry(StringRedisTemplate redis, @Value("${pulseim.im.node-id:node-local}") String nodeId) {
        this.redis = redis;
        this.nodeId = nodeId;
    }

    void online(JwtSupport.Session session) {
        write(session, "ONLINE");
    }

    void heartbeat(JwtSupport.Session session) {
        write(session, "ONLINE");
    }

    void offline(JwtSupport.Session session) {
        try {
            redis.delete(key(session));
        } catch (Exception ignored) {
        }
    }

    private void write(JwtSupport.Session session, String state) {
        try {
            redis.opsForHash().putAll(key(session), Map.of(
                    "userId", session.userId(),
                    "deviceId", session.deviceId(),
                    "nodeId", nodeId,
                    "state", state,
                    "updatedAt", Instant.now().toString()));
            redis.expire(key(session), Duration.ofSeconds(75));
        } catch (Exception ignored) {
        }
    }

    private String key(JwtSupport.Session session) {
        return "pulseim:presence:" + session.userId() + ":" + session.deviceId();
    }
}
