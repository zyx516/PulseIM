package com.pulseim.message;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "conversation_sequences")
class ConversationSequenceEntity {
    @Id
    @Column(name = "conversation_id", length = 80)
    private String conversationId;
    @Column(name = "latest_sequence", nullable = false)
    private long latestSequence;
    @Version
    private long version;

    protected ConversationSequenceEntity() {
    }

    ConversationSequenceEntity(String conversationId) {
        this.conversationId = conversationId;
    }

    long next() {
        latestSequence++;
        return latestSequence;
    }
}
