package com.pulseim.message;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    List<OutboxEventEntity> findTop100ByOrderByCreatedAtDesc();
    List<OutboxEventEntity> findTop50ByStatusOrderByCreatedAtAsc(String status);
}
