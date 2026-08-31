package com.pulseim.message;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface DeliveryEventRepository extends JpaRepository<DeliveryEventEntity, String> {
    List<DeliveryEventEntity> findByMessageIdOrderByOccurredAtAsc(String messageId);
}