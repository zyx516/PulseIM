package com.pulseim.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseim.common.security.JwtSupport;
import com.pulseim.common.ws.WsEnvelope;
import com.pulseim.common.ws.WsEvent;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Connection-only gateway. Business persistence is intentionally outside this process;
 * message persistence and fan-out events are handled by message-service and RabbitMQ.
 */
@Component
public class WebSocketServer implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private final int port;
    private final ObjectMapper mapper;
    private final MessageServiceClient messageServiceClient;
    private final LocalConnectionRegistry connections;
    private final PresenceRegistry presence;
    private final EventLoopGroup boss = new NioEventLoopGroup(1);
    private final EventLoopGroup workers = new NioEventLoopGroup();
    private volatile Channel serverChannel;
    private volatile boolean running;

    public WebSocketServer(@Value("${pulseim.im.port:8090}") int port, ObjectMapper mapper,
                           MessageServiceClient messageServiceClient, LocalConnectionRegistry connections,
                           PresenceRegistry presence) {
        this.port = port;
        this.mapper = mapper;
        this.messageServiceClient = messageServiceClient;
        this.connections = connections;
        this.presence = presence;
    }

    @Override
    public void start() {
        if (running) return;
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            serverChannel = bootstrap.group(boss, workers).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override protected void initChannel(SocketChannel channel) {
                            channel.pipeline().addLast(new HttpServerCodec(), new HttpObjectAggregator(64 * 1024),
                                    new WebSocketServerProtocolHandler("/im/ws", null, true),
                                    new IdleStateHandler(45, 0, 0, TimeUnit.SECONDS),
                                    new ClientFrameHandler(mapper, messageServiceClient, connections, presence));
                        }
                    }).bind(port).sync().channel();
            running = true;
            log.info("PulseIM WebSocket gateway listening on {}", port);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Unable to start WebSocket gateway", exception);
        }
    }

    @Override public void stop() {
        if (serverChannel != null) serverChannel.close();
        boss.shutdownGracefully();
        workers.shutdownGracefully();
        running = false;
    }
    @Override public boolean isRunning() { return running; }
    @Override public boolean isAutoStartup() { return true; }
    @Override public int getPhase() { return 0; }
    @Override public void stop(Runnable callback) { stop(); callback.run(); }

    private static final class ClientFrameHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
        private static final AttributeKey<JwtSupport.Session> SESSION = AttributeKey.valueOf("pulseim.session");
        private static final AttributeKey<String> ACCESS_TOKEN = AttributeKey.valueOf("pulseim.access-token");
        private final ObjectMapper mapper;
        private final MessageServiceClient messageServiceClient;
        private final LocalConnectionRegistry connections;
        private final PresenceRegistry presence;

        private ClientFrameHandler(ObjectMapper mapper, MessageServiceClient messageServiceClient,
                                   LocalConnectionRegistry connections, PresenceRegistry presence) {
            this.mapper = mapper;
            this.messageServiceClient = messageServiceClient;
            this.connections = connections;
            this.presence = presence;
        }

        @Override protected void channelRead0(ChannelHandlerContext context, TextWebSocketFrame frame) {
            try {
                WsEnvelope envelope = mapper.readValue(frame.text(), WsEnvelope.class);
                if (!WsEnvelope.VERSION.equals(envelope.version())) {
                    send(context, envelope.requestId(), "ERROR", Map.of("code", "UNSUPPORTED_VERSION"));
                    return;
                }
                switch (envelope.command()) {
                    case "AUTH" -> authenticate(context, envelope);
                    case "PING" -> {
                        JwtSupport.Session session = context.channel().attr(SESSION).get();
                        if (session != null) presence.heartbeat(session);
                        send(context, envelope.requestId(), "PONG", Map.of("at", Instant.now().toString()));
                    }
                    case "SEND_MESSAGE" -> sendMessage(context, envelope);
                    case "ACK", "READ" -> requireSession(context, envelope.requestId());
                    default -> send(context, envelope.requestId(), "ERROR", Map.of("code", "UNKNOWN_COMMAND"));
                }
            } catch (Exception exception) {
                send(context, null, "ERROR", Map.of("code", "BAD_FRAME", "message", exception.getMessage()));
            }
        }

        private void authenticate(ChannelHandlerContext context, WsEnvelope envelope) {
            JsonNode tokenNode = envelope.data() == null ? null : envelope.data().get("token");
            if (tokenNode == null || tokenNode.asText().isBlank()) {
                send(context, envelope.requestId(), "ERROR", Map.of("code", "AUTH_REQUIRED"));
                return;
            }
            JwtSupport.Session session = JwtSupport.verify(tokenNode.asText());
            context.channel().attr(SESSION).set(session);
            context.channel().attr(ACCESS_TOKEN).set(tokenNode.asText());
            connections.register(session, context.channel());
            presence.online(session);
            send(context, envelope.requestId(), "AUTHENTICATED", Map.of("userId", session.userId(), "deviceId", session.deviceId()));
        }

        private void sendMessage(ChannelHandlerContext context, WsEnvelope envelope) {
            JwtSupport.Session session = requireSession(context, envelope.requestId());
            if (session == null) return;
            JsonNode data = envelope.data();
            if (data == null || missing(data, "clientMessageId") || missing(data, "toUserId") || missing(data, "conversationId") || missing(data, "content")) {
                send(context, envelope.requestId(), "ERROR", Map.of("code", "INVALID_MESSAGE"));
                return;
            }
            MessageServiceClient.SendMessage command = new MessageServiceClient.SendMessage(data.get("clientMessageId").asText(),
                    data.get("conversationId").asText(), data.get("toUserId").asText(), data.get("content").asText());
            messageServiceClient.persist(context.channel().attr(ACCESS_TOKEN).get(), envelope.requestId(), command)
                    .whenComplete((persisted, failure) -> context.executor().execute(() -> completeMessage(context, envelope, session, command, persisted, failure)));
        }

        private void completeMessage(ChannelHandlerContext context, WsEnvelope envelope, JwtSupport.Session session,
                                     MessageServiceClient.SendMessage command, MessageServiceClient.PersistedMessage persisted, Throwable failure) {
            if (!context.channel().isActive()) return;
            if (failure != null) {
                send(context, envelope.requestId(), "ERROR", Map.of("code", "MESSAGE_PERSIST_FAILED"));
                return;
            }
            send(context, envelope.requestId(), "MESSAGE_ACCEPTED", Map.of("clientMessageId", command.clientMessageId(),
                    "messageId", persisted.id(), "sequence", persisted.sequence()));
        }

        private boolean missing(JsonNode data, String name) { return !data.hasNonNull(name) || data.get(name).asText().isBlank(); }
        private JwtSupport.Session requireSession(ChannelHandlerContext context, String requestId) {
            JwtSupport.Session session = context.channel().attr(SESSION).get();
            if (session == null) send(context, requestId, "ERROR", Map.of("code", "AUTH_REQUIRED"));
            return session;
        }
        private void send(ChannelHandlerContext context, String requestId, String type, Object data) {
            context.writeAndFlush(new TextWebSocketFrame(write(WsEvent.of(requestId, type, data))));
        }
        private String write(Object value) {
            try { return mapper.writeValueAsString(value); }
            catch (Exception exception) { return "{\"type\":\"ERROR\",\"data\":{\"code\":\"SERIALIZATION_FAILURE\"}}"; }
        }
        @Override public void userEventTriggered(ChannelHandlerContext context, Object event) {
            if (event instanceof IdleStateEvent) context.close();
        }
        @Override public void channelInactive(ChannelHandlerContext context) {
            JwtSupport.Session session = context.channel().attr(SESSION).get();
            if (session != null) {
                connections.unregister(session, context.channel());
                presence.offline(session);
            }
        }
    }
}
