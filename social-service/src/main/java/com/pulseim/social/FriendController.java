package com.pulseim.social;

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
@RequestMapping("/api/friends")
public class FriendController {
    private final FriendRequestRepository requests;
    private final FriendshipRepository friendships;

    public FriendController(FriendRequestRepository requests, FriendshipRepository friendships) {
        this.requests = requests;
        this.friendships = friendships;
    }

    @GetMapping("/requests")
    public ApiResponse<List<FriendRequestView>> pending(@RequestHeader("Authorization") String authorization,
                                                        @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String userId = BearerTokens.require(authorization).userId();
        return ApiResponse.ok(TraceIds.from(traceId), requests.findByToUserIdAndStatus(userId, "PENDING")
                .stream().map(FriendRequestView::from).toList());
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FriendRequestView> request(@RequestHeader("Authorization") String authorization,
                                                  @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                  @RequestBody FriendRequestCommand command) {
        String userId = BearerTokens.require(authorization).userId();
        FriendRequestEntity result = requests.save(new FriendRequestEntity("fr-" + UUID.randomUUID(), userId,
                command.toUserId(), command.message(), "PENDING", Instant.now()));
        return ApiResponse.ok(TraceIds.from(traceId), FriendRequestView.from(result));
    }

    @PostMapping("/requests/{requestId}/accept")
    @Transactional
    public ApiResponse<FriendRequestView> accept(@RequestHeader("Authorization") String authorization,
                                                 @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                 @PathVariable String requestId) {
        String userId = BearerTokens.require(authorization).userId();
        FriendRequestEntity previous = requests.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Friend request not found"));
        if (!previous.toUserId().equals(userId)) {
            throw new ResponseStatusException(NOT_FOUND, "Friend request not found");
        }
        previous.accept();
        friendships.upsert(previous.fromUserId(), previous.toUserId());
        return ApiResponse.ok(TraceIds.from(traceId), FriendRequestView.from(previous));
    }

    @GetMapping
    public ApiResponse<List<FriendshipView>> friends(@RequestHeader("Authorization") String authorization,
                                                     @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String userId = BearerTokens.require(authorization).userId();
        return ApiResponse.ok(TraceIds.from(traceId), friendships.findByUserId(userId).stream()
                .map(FriendshipView::from).toList());
    }

    @DeleteMapping("/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader("Authorization") String authorization, @PathVariable String friendUserId) {
        String userId = BearerTokens.require(authorization).userId();
        friendships.deletePair(userId, friendUserId);
    }

    public record FriendRequestCommand(String toUserId, String message) {
    }

    public record FriendRequestView(String id, String fromUserId, String toUserId, String message, String status,
                                    Instant createdAt) {
        static FriendRequestView from(FriendRequestEntity entity) {
            return new FriendRequestView(entity.id(), entity.fromUserId(), entity.toUserId(), entity.message(),
                    entity.status(), entity.createdAt());
        }
    }

    public record FriendshipView(String id, String userId, String friendUserId, Instant createdAt) {
        static FriendshipView from(FriendshipEntity entity) {
            return new FriendshipView(entity.id(), entity.userId(), entity.friendUserId(), entity.createdAt());
        }
    }
}
