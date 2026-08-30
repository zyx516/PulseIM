package com.pulseim.social;

import com.pulseim.common.api.ApiResponse;
import com.pulseim.common.api.TraceIds;
import com.pulseim.common.security.BearerTokens;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    private final GroupRepository groups;
    private final GroupMemberRepository members;

    public GroupController(GroupRepository groups, GroupMemberRepository members) {
        this.groups = groups;
        this.members = members;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupView> create(@RequestHeader("Authorization") String authorization,
                                         @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                         @RequestBody CreateGroupCommand command) {
        String ownerId = BearerTokens.require(authorization).userId();
        GroupEntity group = groups.save(new GroupEntity("g-" + UUID.randomUUID(), command.name(), ownerId, Instant.now()));
        members.save(new GroupMemberEntity("gm-" + UUID.randomUUID(), group.id(), ownerId, "OWNER", Instant.now()));
        return ApiResponse.ok(TraceIds.from(traceId), GroupView.from(group));
    }

    @GetMapping
    public ApiResponse<List<GroupView>> myGroups(@RequestHeader("Authorization") String authorization,
                                                @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        String userId = BearerTokens.require(authorization).userId();
        List<String> groupIds = members.findByUserId(userId).stream().map(GroupMemberEntity::groupId).toList();
        return ApiResponse.ok(TraceIds.from(traceId), groups.findAllById(groupIds).stream().map(GroupView::from).toList());
    }

    @PostMapping("/{groupId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupMemberView> addMember(@RequestHeader("Authorization") String authorization,
                                                  @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                  @PathVariable String groupId,
                                                  @RequestBody AddMemberCommand command) {
        BearerTokens.require(authorization);
        GroupMemberEntity member = members.save(new GroupMemberEntity("gm-" + UUID.randomUUID(), groupId,
                command.userId(), command.role() == null || command.role().isBlank() ? "MEMBER" : command.role(), Instant.now()));
        return ApiResponse.ok(TraceIds.from(traceId), GroupMemberView.from(member));
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<List<GroupMemberView>> groupMembers(@RequestHeader("Authorization") String authorization,
                                                           @RequestHeader(value = "X-Trace-Id", required = false) String traceId,
                                                           @PathVariable String groupId) {
        BearerTokens.require(authorization);
        return ApiResponse.ok(TraceIds.from(traceId), members.findByGroupId(groupId).stream().map(GroupMemberView::from).toList());
    }

    public record CreateGroupCommand(String name) {
    }

    public record AddMemberCommand(String userId, String role) {
    }

    public record GroupView(String id, String name, String ownerId, Instant createdAt) {
        static GroupView from(GroupEntity entity) {
            return new GroupView(entity.id(), entity.name(), entity.ownerId(), entity.createdAt());
        }
    }

    public record GroupMemberView(String id, String groupId, String userId, String role, Instant joinedAt) {
        static GroupMemberView from(GroupMemberEntity entity) {
            return new GroupMemberView(entity.id(), entity.groupId(), entity.userId(), entity.role(), entity.joinedAt());
        }
    }
}
