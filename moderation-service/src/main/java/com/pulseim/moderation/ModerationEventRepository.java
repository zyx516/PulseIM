package com.pulseim.moderation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ModerationEventRepository extends JpaRepository<ModerationEventEntity, String> {
    List<ModerationEventEntity> findTop50ByOrderByCreatedAtDesc();
}
