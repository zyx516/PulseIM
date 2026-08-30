package com.pulseim.conversation;

import org.springframework.data.jpa.repository.JpaRepository;

interface ConversationRepository extends JpaRepository<ConversationEntity, String> {
}
