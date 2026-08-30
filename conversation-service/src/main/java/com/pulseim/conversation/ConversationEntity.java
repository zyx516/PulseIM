package com.pulseim.conversation;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "conversations")
class ConversationEntity {
    @Id
    private String id;
    @Column(nullable = false, length = 20)
    private String type;
    @Column(name = "member_a", length = 80)
    private String memberA;
    @Column(name = "member_b", length = 80)
    private String memberB;
    @Column(name = "group_id", length = 80)
    private String groupId;
    @Column(name = "latest_sequence", nullable = false)
    private long latestSequence;
    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview = "";
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConversationEntity() {
    }

    ConversationEntity(String id, String type, String memberA, String memberB, String groupId, Instant createdAt) {
        this.id = id;
        this.type = type;
        this.memberA = memberA;
        this.memberB = memberB;
        this.groupId = groupId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    void updateLatest(long latestSequence, String preview) {
        this.latestSequence = Math.max(this.latestSequence, latestSequence);
        this.lastMessagePreview = preview == null ? "" : preview;
        this.updatedAt = Instant.now();
    }

    String id() { return id; }
    String type() { return type; }
    String memberA() { return memberA; }
    String memberB() { return memberB; }
    String groupId() { return groupId; }
    long latestSequence() { return latestSequence; }
    String lastMessagePreview() { return lastMessagePreview; }
    Instant updatedAt() { return updatedAt; }
    Instant createdAt() { return createdAt; }
}
