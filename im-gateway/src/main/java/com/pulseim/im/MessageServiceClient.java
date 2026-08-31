package com.pulseim.im;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
public class MessageServiceClient {
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper; private final String messageServiceUri;
    public MessageServiceClient(ObjectMapper mapper, @Value("${pulseim.message-service-uri:http://localhost:8085}") String messageServiceUri) { this.mapper = mapper; this.messageServiceUri = messageServiceUri; }
    public CompletableFuture<PersistedMessage> persist(String accessToken, String requestId, SendMessage command) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(messageServiceUri + "/api/messages")).timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json").header("Authorization", "Bearer " + accessToken).header("X-Trace-Id", requestId).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(command))).build();
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> { if (response.statusCode() / 100 != 2) throw new IllegalStateException("message-service returned " + response.statusCode()); try { JsonNode data = mapper.readTree(response.body()).path("data"); return new PersistedMessage(data.path("id").asText(), data.path("sequence").asLong(), data.path("createdAt").asText()); } catch (Exception exception) { throw new IllegalStateException("Invalid message-service response", exception); } });
        } catch (Exception exception) { return CompletableFuture.failedFuture(exception); }
    }
    public void acknowledge(String accessToken, String requestId, String messageId, String stage) {
        try { HttpRequest request = HttpRequest.newBuilder(URI.create(messageServiceUri + "/api/messages/" + messageId + "/ack")).timeout(Duration.ofSeconds(3)).header("Content-Type", "application/json").header("Authorization", "Bearer " + accessToken).header("X-Trace-Id", requestId).POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(new Acknowledge(stage)))).build(); httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding()); } catch (Exception ignored) { }
    }
    public record SendMessage(String clientMessageId, String conversationId, String toUserId, String content) { }
    public record PersistedMessage(String id, long sequence, String createdAt) { }
    private record Acknowledge(String stage) { }
}