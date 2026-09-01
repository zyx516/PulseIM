package com.pulseim.common.mq;
import java.time.Instant;
public record GroupMemberChangedEvent(String eventId,String groupId,String userId,String action,Instant occurredAt){}