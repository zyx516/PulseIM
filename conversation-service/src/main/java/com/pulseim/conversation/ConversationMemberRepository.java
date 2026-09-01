package com.pulseim.conversation;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, String> {
    List<ConversationMemberEntity> findByUserIdOrderByPinnedDescUpdatedAtDesc(String userId);
    Optional<ConversationMemberEntity> findByConversationIdAndUserId(String conversationId, String userId);
    List<ConversationMemberEntity> findByConversationId(String conversationId);

    default void ensureMember(String conversationId, String userId) {
        try {
            findByConversationIdAndUserId(conversationId, userId).orElseGet(() ->
                    save(new ConversationMemberEntity("cm-" + UUID.randomUUID(), conversationId, userId, Instant.now())));
        } catch (DataIntegrityViolationException ignored) {
            // Another consumer inserted the same member first; the event is already applied.
        }
    }
}
