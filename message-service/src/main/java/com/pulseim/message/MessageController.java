package com.pulseim.message;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.api.TraceIds;
import com.pulseim.common.security.BearerTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageView> send(@RequestHeader("Authorization") String authorization,
                                         @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                         @Valid @RequestBody SendMessageCommand command) {
        String senderId = BearerTokens.require(authorization).userId();
        return ApiResponse.ok(TraceIds.from(traceId), messageService.persist(senderId, command));
    }

    @GetMapping
    public ApiResponse<List<MessageView>> history(@RequestHeader("Authorization") String authorization,
                                                  @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                  @RequestParam String conversationId,
                                                  @RequestParam(defaultValue = "0") long afterSequence) {
        BearerTokens.require(authorization);
        return ApiResponse.ok(TraceIds.from(traceId), messageService.history(conversationId, afterSequence));
    }

    @PostMapping("/{messageId}/recall")
    public ApiResponse<MessageView> recall(@RequestHeader("Authorization") String authorization,
                                           @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                           @PathVariable String messageId) {
        String userId = BearerTokens.require(authorization).userId();
        return ApiResponse.ok(TraceIds.from(traceId), messageService.recall(userId, messageId));
    }

    @GetMapping("/outbox")
    public ApiResponse<List<OutboxEventView>> outbox(@RequestHeader("Authorization") String authorization,
                                                     @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        BearerTokens.require(authorization);
        return ApiResponse.ok(TraceIds.from(traceId), messageService.outbox());
    }

    public record SendMessageCommand(@NotBlank String clientMessageId, @NotBlank String conversationId,
                                     @NotBlank String toUserId, @NotBlank @Size(max = 4000) String content) {
    }
}
