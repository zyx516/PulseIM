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

@RestController @RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;
    public MessageController(MessageService messageService) { this.messageService = messageService; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<MessageView> send(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Trace-Id", required = false) String traceId, @Valid @RequestBody SendMessageCommand command) { return ApiResponse.ok(TraceIds.from(traceId), messageService.persist(BearerTokens.require(authorization).userId(), command)); }
    @GetMapping public ApiResponse<List<MessageView>> history(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Trace-Id", required = false) String traceId, @RequestParam String conversationId, @RequestParam(defaultValue = "0") long afterSequence) { BearerTokens.require(authorization); return ApiResponse.ok(TraceIds.from(traceId), messageService.history(conversationId, afterSequence)); }
    @PostMapping("/{messageId}/recall") public ApiResponse<MessageView> recall(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Trace-Id", required = false) String traceId, @PathVariable String messageId) { return ApiResponse.ok(TraceIds.from(traceId), messageService.recall(BearerTokens.require(authorization).userId(), messageId)); }
    @GetMapping("/{messageId}/delivery") public ApiResponse<List<DeliveryEventView>> delivery(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Trace-Id", required = false) String traceId, @PathVariable String messageId) { return ApiResponse.ok(TraceIds.from(traceId), messageService.delivery(BearerTokens.require(authorization).userId(), messageId)); }
    @GetMapping("/{messageId}/outbox") public ApiResponse<List<OutboxEventView>> outbox(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Trace-Id", required = false) String traceId, @PathVariable String messageId) { return ApiResponse.ok(TraceIds.from(traceId), messageService.outboxForMessage(BearerTokens.require(authorization).userId(), messageId)); }
    @PostMapping("/outbox/{eventId}/retry") public ApiResponse<Void> retry(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Trace-Id", required = false) String traceId, @PathVariable String eventId) { messageService.retryOutbox(BearerTokens.require(authorization).userId(), eventId); return ApiResponse.ok(TraceIds.from(traceId), null); }
    @PostMapping("/{messageId}/ack") public ApiResponse<Void> acknowledge(@RequestHeader("Authorization") String authorization, @RequestHeader(value = "X-Trace-Id", required = false) String traceId, @PathVariable String messageId, @RequestBody(required = false) AckCommand command) { messageService.acknowledge(BearerTokens.require(authorization).userId(), messageId, command == null ? "ACK" : command.stage()); return ApiResponse.ok(TraceIds.from(traceId), null); }
    public record SendMessageCommand(@NotBlank String clientMessageId, @NotBlank String conversationId, @NotBlank String toUserId, @NotBlank @Size(max = 4000) String content) { }
    public record AckCommand(String stage) { }
}