package com.pulseim.conversation;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "conversation_members",
        uniqueConstraints = @UniqueConstraint(name = "uk_conversation_user", columnNames = {"conversation_id", "user_id"}),
        indexes = @Index(name = "idx_member_user", columnList = "user_id, pinned, updated_at"))
class ConversationMemberEntity {
    @Id
    private String id;
    @Column(name = "conversation_id", nullable = false, length = 180)
    private String conversationId;
    @Column(name = "user_id", nullable = false, length = 80)
    private String userId;
    @Column(name = "read_sequence", nullable = false)
    private long readSequence;
    @Column(nullable = false)
    private boolean pinned;
    @Column(nullable = false)
    private boolean muted;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ConversationMemberEntity() {
    }

    ConversationMemberEntity(String id, String conversationId, String userId, Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.userId = userId;
        this.updatedAt = createdAt;
    }

    void readTo(long sequence) {
        this.readSequence = sequence;
        this.updatedAt = Instant.now();
    }

    void updateSettings(boolean pinned, boolean muted) {
        this.pinned = pinned;
        this.muted = muted;
        this.updatedAt = Instant.now();
    }

    String id() { return id; }
    String conversationId() { return conversationId; }
    String userId() { return userId; }
    long readSequence() { return readSequence; }
    boolean pinned() { return pinned; }
    boolean muted() { return muted; }
    Instant updatedAt() { return updatedAt; }
}
