package com.pulseim.social;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "friend_requests", indexes = @Index(name = "idx_request_to_status", columnList = "to_user_id, status"))
class FriendRequestEntity {
    @Id
    private String id;
    @Column(name = "from_user_id", nullable = false, length = 80)
    private String fromUserId;
    @Column(name = "to_user_id", nullable = false, length = 80)
    private String toUserId;
    @Column(length = 200)
    private String message;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FriendRequestEntity() {
    }

    FriendRequestEntity(String id, String fromUserId, String toUserId, String message, String status, Instant createdAt) {
        this.id = id;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.message = message == null ? "" : message;
        this.status = status;
        this.createdAt = createdAt;
    }

    void accept() { this.status = "ACCEPTED"; }
    String id() { return id; }
    String fromUserId() { return fromUserId; }
    String toUserId() { return toUserId; }
    String message() { return message; }
    String status() { return status; }
    Instant createdAt() { return createdAt; }
}
