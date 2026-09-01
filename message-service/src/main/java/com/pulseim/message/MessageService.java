package com.pulseim.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseim.common.mq.MessagePersistedEvent;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
class MessageService {
    private final MessageRepository messages;
    private final ConversationSequenceRepository sequences;
    private final OutboxEventRepository outboxEvents;
    private final DeliveryEventRepository deliveryEvents;
    private final ObjectMapper mapper; private final ConversationMemberClient conversationMembers;
    MessageService(MessageRepository messages, ConversationSequenceRepository sequences, OutboxEventRepository outboxEvents, DeliveryEventRepository deliveryEvents, ObjectMapper mapper, ConversationMemberClient conversationMembers) {
        this.messages = messages; this.sequences = sequences; this.outboxEvents = outboxEvents; this.deliveryEvents = deliveryEvents; this.mapper = mapper; this.conversationMembers = conversationMembers;
    }
    @Transactional public MessageView persist(String senderId, String authorization, MessageController.SendMessageCommand command) {
        return messages.findBySenderIdAndClientMessageId(senderId, command.clientMessageId()).map(MessageView::from).orElseGet(() -> create(senderId, authorization, command));
    }
    private MessageView create(String senderId, String authorization, MessageController.SendMessageCommand command) {
        try {
            ConversationSequenceEntity sequence = sequences.findByConversationId(command.conversationId()).orElseGet(() -> sequences.saveAndFlush(new ConversationSequenceEntity(command.conversationId())));
            Instant now = Instant.now();
            MessageEntity saved = messages.saveAndFlush(new MessageEntity("m-" + UUID.randomUUID(), command.clientMessageId(), command.conversationId(), senderId, command.toUserId(), command.content(), sequence.next(), now, "NORMAL", command.replyToMessageId(), command.replyPreview(), command.mentions()));
            MessagePersistedEvent event = new MessagePersistedEvent("evt-" + UUID.randomUUID(), saved.id(), saved.clientMessageId(), saved.conversationId(), saved.senderId(), saved.toUserId(), saved.content(), saved.sequence(), saved.createdAt(), Instant.now());
            if (saved.conversationId().startsWith("gc-")) { conversationMembers.members(authorization, saved.conversationId()).stream().filter(id -> !id.equals(senderId)).forEach(target -> { MessagePersistedEvent fanout = new MessagePersistedEvent("evt-" + UUID.randomUUID(), saved.id(), saved.clientMessageId(), saved.conversationId(), saved.senderId(), target, saved.content(), saved.sequence(), saved.createdAt(), Instant.now()); outboxEvents.save(new OutboxEventEntity(fanout.eventId(), "MESSAGE_PERSISTED", saved.id(), write(fanout), fanout.eventCreatedAt())); }); } else outboxEvents.save(new OutboxEventEntity(event.eventId(), "MESSAGE_PERSISTED", saved.id(), write(event), event.eventCreatedAt()));
            record(saved.id(), "PERSISTED", "Message and Outbox committed together"); record(saved.id(), "OUTBOX_PENDING", "Waiting for RabbitMQ publication");
            return MessageView.from(saved);
        } catch (DataIntegrityViolationException duplicate) { return messages.findBySenderIdAndClientMessageId(senderId, command.clientMessageId()).map(MessageView::from).orElseThrow(() -> duplicate); }
    }
    public List<MessageView> history(String conversationId, long afterSequence) { return messages.findTop50ByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(conversationId, afterSequence).stream().map(MessageView::from).toList(); }
    public DashboardView dashboard(String userId) { java.util.List<MessageEntity> related = messages.findTop200RelatedTo(userId); java.util.List<String> ids = related.stream().map(MessageEntity::id).toList(); java.util.List<DeliveryEventEntity> events = ids.isEmpty() ? java.util.List.of() : deliveryEvents.findByMessageIdInOrderByOccurredAtDesc(ids); java.util.Map<String,Long> stages = events.stream().collect(java.util.stream.Collectors.groupingBy(DeliveryEventEntity::stage, java.util.LinkedHashMap::new, java.util.stream.Collectors.counting())); java.util.List<DeliveryEventView> failures = events.stream().filter(e -> e.stage().contains("RETRY") || e.stage().contains("DEAD")).limit(8).map(DeliveryEventView::from).toList(); return new DashboardView(related.size(), stages, failures); }
    @Transactional public MessageView recall(String userId, String messageId) { MessageEntity message = findMessage(messageId); if (!message.senderId().equals(userId)) throw new ResponseStatusException(FORBIDDEN, "Only the sender can recall this message"); message.recall(); record(messageId, "RECALLED", "Recalled by sender"); return MessageView.from(message); }
    public List<DeliveryEventView> delivery(String userId, String authorization, String messageId) { MessageEntity message = findMessage(messageId); requireParticipant(userId, authorization, message); return deliveryEvents.findByMessageIdOrderByOccurredAtAsc(messageId).stream().map(DeliveryEventView::from).toList(); }
    public List<OutboxEventView> outboxForMessage(String userId, String authorization, String messageId) { MessageEntity message = findMessage(messageId); requireParticipant(userId, authorization, message); return outboxEvents.findByAggregateIdOrderByCreatedAtDesc(messageId).stream().map(OutboxEventView::from).toList(); }
    @Transactional public void acknowledge(String userId, String authorization, String messageId, String stage) { MessageEntity message = findMessage(messageId); requireParticipant(userId, authorization, message); record(messageId, "READ".equals(stage) ? "READ" : "CLIENT_ACK", userId); }
    @Transactional public void retryOutbox(String userId, String eventId) { OutboxEventEntity event = outboxEvents.findById(eventId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Outbox event not found")); MessageEntity message = findMessage(event.aggregateId()); if (!message.senderId().equals(userId)) throw new ResponseStatusException(FORBIDDEN, "Only the sender can retry this event"); event.retry(); record(event.aggregateId(), "OUTBOX_REQUEUED", "Retried by sender"); }
    @Transactional void recordPublished(String messageId) { record(messageId, "MQ_PUBLISHED", "Published to RabbitMQ"); }
    @Transactional void recordPublishFailure(String messageId, String detail, boolean dead) { record(messageId, dead ? "DEAD_LETTER" : "OUTBOX_RETRY", detail); }
    @Transactional void record(String messageId, String stage, String detail) { deliveryEvents.save(new DeliveryEventEntity("de-" + UUID.randomUUID(), messageId, stage, detail)); }
    private MessageEntity findMessage(String messageId) { return messages.findById(messageId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Message not found")); }
    private void requireParticipant(String userId, String authorization, MessageEntity message) { if (userId.equals(message.senderId()) || userId.equals(message.toUserId())) return; if (message.conversationId().startsWith("gc-") && conversationMembers.members(authorization, message.conversationId()).contains(userId)) return; throw new ResponseStatusException(FORBIDDEN, "Message participant required"); }
    private String write(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException("Unable to serialize outbox event", exception); } }
}