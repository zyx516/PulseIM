package com.pulseim.message;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "im_messages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sender_client_msg", columnNames = {"sender_id", "client_message_id"}),
                @UniqueConstraint(name = "uk_conversation_sequence", columnNames = {"conversation_id", "sequence"})
        },
        indexes = @Index(name = "idx_conversation_sequence", columnList = "conversation_id, sequence"))
class MessageEntity {
    @Id
    private String id;
    @Column(name = "client_message_id", nullable = false, length = 80)
    private String clientMessageId;
    @Column(name = "conversation_id", nullable = false, length = 180)
    private String conversationId;
    @Column(name = "sender_id", nullable = false, length = 80)
    private String senderId;
    @Column(name = "to_user_id", nullable = false, length = 80)
    private String toUserId;
    @Column(nullable = false, length = 4000)
    private String content;
    @Column(name = "reply_to_message_id", length = 80)
    private String replyToMessageId;
    @Column(name = "reply_preview", length = 160)
    private String replyPreview;
    @Column(name = "mentions", length = 1000)
    private String mentions;
    @Column(nullable = false)
    private long sequence;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "recalled_at")
    private Instant recalledAt;

    protected MessageEntity() {
    }

    MessageEntity(String id, String clientMessageId, String conversationId, String senderId, String toUserId,
                  String content, long sequence, Instant createdAt, String status, String replyToMessageId, String replyPreview, String mentions) {
        this.id = id;
        this.clientMessageId = clientMessageId;
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.toUserId = toUserId;
        this.content = content;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.status = status; this.replyToMessageId = replyToMessageId; this.replyPreview = replyPreview; this.mentions = mentions;
    }

    String id() { return id; }
    String clientMessageId() { return clientMessageId; }
    String conversationId() { return conversationId; }
    String senderId() { return senderId; }
    String toUserId() { return toUserId; }
    String content() { return content; }
    long sequence() { return sequence; }
    Instant createdAt() { return createdAt; }
    String status() { return status; }
    Instant recalledAt() { return recalledAt; } String replyToMessageId() { return replyToMessageId; } String replyPreview() { return replyPreview; } String mentions() { return mentions; }

    void recall() {
        this.status = "RECALLED";
        this.recalledAt = Instant.now();
    }
}
