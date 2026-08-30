package com.pulseim.social;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "friendships", uniqueConstraints = @UniqueConstraint(name = "uk_user_friend", columnNames = {"user_id", "friend_user_id"}))
class FriendshipEntity {
    @Id
    private String id;
    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;
    @Column(name = "friend_user_id", nullable = false, length = 80)
    private String friendUserId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FriendshipEntity() {
    }

    FriendshipEntity(String id, String userId, String friendUserId, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.friendUserId = friendUserId;
        this.createdAt = createdAt;
    }

    String id() { return id; }
    String userId() { return userId; }
    String friendUserId() { return friendUserId; }
    Instant createdAt() { return createdAt; }
}
