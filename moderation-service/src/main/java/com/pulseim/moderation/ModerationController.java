package com.pulseim.moderation;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.api.TraceIds;
import com.pulseim.common.security.BearerTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {
    private static final List<String> BLOCKED_WORDS = List.of("spam", "blocked");
    private final ModerationEventRepository events;

    public ModerationController(ModerationEventRepository events) {
        this.events = events;
    }

    @PostMapping("/check")
    public ApiResponse<ModerationView> check(@RequestHeader("Authorization") String authorization,
                                             @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                             @Valid @RequestBody ModerationCommand command) {
        String userId = BearerTokens.require(authorization).userId();
        String lower = command.content().toLowerCase();
        boolean allowed = BLOCKED_WORDS.stream().noneMatch(lower::contains);
        ModerationEventEntity event = events.save(new ModerationEventEntity("mod-" + UUID.randomUUID(), userId,
                command.conversationId(), allowed ? "PASSED" : "REJECTED", allowed ? "" : "KEYWORD", Instant.now()));
        return ApiResponse.ok(TraceIds.from(traceId), ModerationView.from(event));
    }

    @GetMapping("/events")
    public ApiResponse<List<ModerationView>> recent(@RequestHeader("Authorization") String authorization,
                                                    @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        BearerTokens.require(authorization);
        return ApiResponse.ok(TraceIds.from(traceId), events.findTop50ByOrderByCreatedAtDesc().stream().map(ModerationView::from).toList());
    }

    public record ModerationCommand(@NotBlank String conversationId, @NotBlank String content) {
    }

    public record ModerationView(String id, String userId, String conversationId, String result, String reason,
                                 Instant createdAt) {
        static ModerationView from(ModerationEventEntity entity) {
            return new ModerationView(entity.id(), entity.userId(), entity.conversationId(), entity.result(),
                    entity.reason(), entity.createdAt());
        }
    }
}
