package com.pulseim.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ConversationMemberRepository extends JpaRepository<ConversationMemberEntity, String> {
    List<ConversationMemberEntity> findByUserIdOrderByPinnedDescUpdatedAtDesc(String userId);
    Optional<ConversationMemberEntity> findByConversationIdAndUserId(String conversationId, String userId);

    default void ensureMember(String conversationId, String userId) {
        findByConversationIdAndUserId(conversationId, userId).orElseGet(() ->
                save(new ConversationMemberEntity("cm-" + UUID.randomUUID(), conversationId, userId, Instant.now())));
    }
}
