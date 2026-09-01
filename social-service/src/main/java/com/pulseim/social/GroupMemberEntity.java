package com.pulseim.social;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "group_members", uniqueConstraints = @UniqueConstraint(name = "uk_group_member", columnNames = {"group_id", "user_id"}))
class GroupMemberEntity {
    @Id
    private String id;
    @Column(name = "group_id", nullable = false, length = 80)
    private String groupId;
    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;
    @Column(nullable = false, length = 20)
    private String role;
    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected GroupMemberEntity() {
    }

    GroupMemberEntity(String id, String groupId, String userId, String role, Instant joinedAt) {
        this.id = id;
        this.groupId = groupId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    String id() { return id; }
    String groupId() { return groupId; }
    String userId() { return userId; }
    String role() { return role; }
    Instant joinedAt() { return joinedAt; }
    void changeRole(String role) { this.role = role; }
}
