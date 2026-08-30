package com.pulseim.conversation;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.api.TraceIds;
import com.pulseim.common.security.BearerTokens;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationRepository conversations;
    private final ConversationMemberRepository members;

    public ConversationController(ConversationRepository conversations, ConversationMemberRepository members) {
        this.conversations = conversations;
        this.members = members;
    }

    @GetMapping
    public ApiResponse<List<ConversationView>> list(@RequestHeader("Authorization") String authorization,
                                                    @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String userId = BearerTokens.require(authorization).userId();
        return ApiResponse.ok(TraceIds.from(traceId), members.findByUserIdOrderByPinnedDescUpdatedAtDesc(userId).stream()
                .map(member -> ConversationView.from(conversations.findById(member.conversationId()).orElseThrow(), member)).toList());
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<ConversationView> createDirect(@RequestHeader("Authorization") String authorization,
                                                      @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                      @RequestBody DirectConversationCommand command) {
        String userId = BearerTokens.require(authorization).userId();
        String conversationId = directConversationId(userId, command.peerUserId());
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseGet(() -> conversations.save(new ConversationEntity(conversationId, "DIRECT", userId, command.peerUserId(), null, Instant.now())));
        members.ensureMember(conversation.id(), userId);
        members.ensureMember(conversation.id(), command.peerUserId());
        ConversationMemberEntity member = members.findByConversationIdAndUserId(conversation.id(), userId).orElseThrow();
        return ApiResponse.ok(TraceIds.from(traceId), ConversationView.from(conversation, member));
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ApiResponse<ConversationView> createGroupConversation(@RequestHeader("Authorization") String authorization,
                                                                @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                                @RequestBody GroupConversationCommand command) {
        String userId = BearerTokens.require(authorization).userId();
        String conversationId = "gc-" + command.groupId();
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseGet(() -> conversations.save(new ConversationEntity(conversationId, "GROUP", null, null, command.groupId(), Instant.now())));
        members.ensureMember(conversation.id(), userId);
        ConversationMemberEntity member = members.findByConversationIdAndUserId(conversation.id(), userId).orElseThrow();
        return ApiResponse.ok(TraceIds.from(traceId), ConversationView.from(conversation, member));
    }

    @PostMapping("/{conversationId}/read")
    @Transactional
    public ApiResponse<ConversationView> read(@RequestHeader("Authorization") String authorization,
                                              @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                              @PathVariable String conversationId,
                                              @RequestBody ReadCommand command) {
        String userId = BearerTokens.require(authorization).userId();
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Conversation not found"));
        ConversationMemberEntity member = members.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Conversation member not found"));
        member.readTo(Math.max(member.readSequence(), command.readSequence()));
        return ApiResponse.ok(TraceIds.from(traceId), ConversationView.from(conversation, member));
    }

    @PostMapping("/{conversationId}/settings")
    @Transactional
    public ApiResponse<ConversationView> settings(@RequestHeader("Authorization") String authorization,
                                                  @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                  @PathVariable String conversationId,
                                                  @RequestBody ConversationSettingsCommand command) {
        String userId = BearerTokens.require(authorization).userId();
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Conversation not found"));
        ConversationMemberEntity member = members.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Conversation member not found"));
        member.updateSettings(Boolean.TRUE.equals(command.pinned()), Boolean.TRUE.equals(command.muted()));
        return ApiResponse.ok(TraceIds.from(traceId), ConversationView.from(conversation, member));
    }

    @PostMapping("/{conversationId}/projection")
    @Transactional
    public ApiResponse<ConversationView> updateProjection(@RequestHeader("Authorization") String authorization,
                                                          @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                          @PathVariable String conversationId,
                                                          @RequestBody ProjectionCommand command) {
        String userId = BearerTokens.require(authorization).userId();
        ConversationEntity conversation = conversations.findById(conversationId)
                .orElseGet(() -> conversations.save(new ConversationEntity(conversationId, "DIRECT", userId, command.peerUserId(), null, Instant.now())));
        conversation.updateLatest(command.latestSequence(), command.lastMessagePreview());
        members.ensureMember(conversation.id(), userId);
        ConversationMemberEntity member = members.findByConversationIdAndUserId(conversation.id(), userId).orElseThrow();
        return ApiResponse.ok(TraceIds.from(traceId), ConversationView.from(conversation, member));
    }

    private static String directConversationId(String left, String right) {
        return left.compareTo(right) <= 0 ? "direct-" + left + "-" + right : "direct-" + right + "-" + left;
    }

    public record DirectConversationCommand(String peerUserId) {
    }

    public record GroupConversationCommand(String groupId) {
    }

    public record ReadCommand(long readSequence) {
    }

    public record ConversationSettingsCommand(Boolean pinned, Boolean muted) {
    }

    public record ProjectionCommand(String peerUserId, long latestSequence, String lastMessagePreview) {
    }

    public record ConversationView(String id, String type, String memberA, String memberB, String groupId,
                                   long latestSequence, long readSequence, long unreadCount, boolean pinned,
                                   boolean muted, String lastMessagePreview, Instant updatedAt, Instant createdAt) {
        static ConversationView from(ConversationEntity conversation, ConversationMemberEntity member) {
            long unread = Math.max(0, conversation.latestSequence() - member.readSequence());
            return new ConversationView(conversation.id(), conversation.type(), conversation.memberA(), conversation.memberB(),
                    conversation.groupId(), conversation.latestSequence(), member.readSequence(), unread, member.pinned(),
                    member.muted(), conversation.lastMessagePreview(), conversation.updatedAt(), conversation.createdAt());
        }
    }
}
