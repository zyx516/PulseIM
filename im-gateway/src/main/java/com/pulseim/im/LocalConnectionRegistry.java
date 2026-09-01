package com.pulseim.im;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseim.common.mq.MessagePersistedEvent;
import com.pulseim.common.security.JwtSupport;
import com.pulseim.common.ws.WsEvent;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
class LocalConnectionRegistry {
    private final Map<String, ChannelGroup> channelsByUser = new ConcurrentHashMap<>();
    private final Set<String> pushedMessages = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper;

    LocalConnectionRegistry(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    void register(JwtSupport.Session session, Channel channel) {
        channelsByUser.computeIfAbsent(session.userId(), ignored -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)).add(channel);
    }

    void unregister(JwtSupport.Session session, Channel channel) {
        ChannelGroup channels = channelsByUser.get(session.userId());
        if (channels != null) {
            channels.remove(channel);
        }
    }

    void push(MessagePersistedEvent event, String requestId) {
        if (!pushedMessages.add(event.messageId() + ":" + event.toUserId())) {
            return;
        }
        ChannelGroup targets = channelsByUser.get(event.toUserId());
        if (targets == null || targets.isEmpty()) {
            return;
        }
        Map<String, Object> payload = Map.of("id", event.messageId(), "clientMessageId", event.clientMessageId(),
                "conversationId", event.conversationId(), "fromUserId", event.senderId(), "content", event.content(),
                "sequence", event.sequence(), "sentAt", event.createdAt().toString());
        targets.writeAndFlush(new TextWebSocketFrame(write(WsEvent.of(requestId, "MESSAGE_EVENT", payload))));
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{\"type\":\"ERROR\",\"data\":{\"code\":\"SERIALIZATION_FAILURE\"}}";
        }
    }
}
