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
    private final ObjectMapper mapper;
    MessageService(MessageRepository messages, ConversationSequenceRepository sequences, OutboxEventRepository outboxEvents, DeliveryEventRepository deliveryEvents, ObjectMapper mapper) {
        this.messages = messages; this.sequences = sequences; this.outboxEvents = outboxEvents; this.deliveryEvents = deliveryEvents; this.mapper = mapper;
    }
    @Transactional public MessageView persist(String senderId, MessageController.SendMessageCommand command) {
        return messages.findBySenderIdAndClientMessageId(senderId, command.clientMessageId()).map(MessageView::from).orElseGet(() -> create(senderId, command));
    }
    private MessageView create(String senderId, MessageController.SendMessageCommand command) {
        try {
            ConversationSequenceEntity sequence = sequences.findByConversationId(command.conversationId()).orElseGet(() -> sequences.saveAndFlush(new ConversationSequenceEntity(command.conversationId())));
            Instant now = Instant.now();
            MessageEntity saved = messages.saveAndFlush(new MessageEntity("m-" + UUID.randomUUID(), command.clientMessageId(), command.conversationId(), senderId, command.toUserId(), command.content(), sequence.next(), now, "NORMAL"));
            MessagePersistedEvent event = new MessagePersistedEvent("evt-" + UUID.randomUUID(), saved.id(), saved.clientMessageId(), saved.conversationId(), saved.senderId(), saved.toUserId(), saved.content(), saved.sequence(), saved.createdAt(), Instant.now());
            outboxEvents.save(new OutboxEventEntity(event.eventId(), "MESSAGE_PERSISTED", saved.id(), write(event), event.eventCreatedAt()));
            record(saved.id(), "PERSISTED", "Message and Outbox committed together"); record(saved.id(), "OUTBOX_PENDING", "Waiting for RabbitMQ publication");
            return MessageView.from(saved);
        } catch (DataIntegrityViolationException duplicate) { return messages.findBySenderIdAndClientMessageId(senderId, command.clientMessageId()).map(MessageView::from).orElseThrow(() -> duplicate); }
    }
    public List<MessageView> history(String conversationId, long afterSequence) { return messages.findTop50ByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(conversationId, afterSequence).stream().map(MessageView::from).toList(); }
    @Transactional public MessageView recall(String userId, String messageId) { MessageEntity message = findMessage(messageId); if (!message.senderId().equals(userId)) throw new ResponseStatusException(FORBIDDEN, "Only the sender can recall this message"); message.recall(); record(messageId, "RECALLED", "Recalled by sender"); return MessageView.from(message); }
    public List<DeliveryEventView> delivery(String userId, String messageId) { requireParticipant(userId, findMessage(messageId)); return deliveryEvents.findByMessageIdOrderByOccurredAtAsc(messageId).stream().map(DeliveryEventView::from).toList(); }
    public List<OutboxEventView> outboxForMessage(String userId, String messageId) { requireParticipant(userId, findMessage(messageId)); return outboxEvents.findByAggregateIdOrderByCreatedAtDesc(messageId).stream().map(OutboxEventView::from).toList(); }
    @Transactional public void acknowledge(String userId, String messageId, String stage) { requireParticipant(userId, findMessage(messageId)); record(messageId, "READ".equals(stage) ? "READ" : "CLIENT_ACK", userId); }
    @Transactional public void retryOutbox(String userId, String eventId) { OutboxEventEntity event = outboxEvents.findById(eventId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Outbox event not found")); MessageEntity message = findMessage(event.aggregateId()); if (!message.senderId().equals(userId)) throw new ResponseStatusException(FORBIDDEN, "Only the sender can retry this event"); event.retry(); record(event.aggregateId(), "OUTBOX_REQUEUED", "Retried by sender"); }
    @Transactional void recordPublished(String messageId) { record(messageId, "MQ_PUBLISHED", "Published to RabbitMQ"); }
    @Transactional void recordPublishFailure(String messageId, String detail, boolean dead) { record(messageId, dead ? "DEAD_LETTER" : "OUTBOX_RETRY", detail); }
    @Transactional void record(String messageId, String stage, String detail) { deliveryEvents.save(new DeliveryEventEntity("de-" + UUID.randomUUID(), messageId, stage, detail)); }
    private MessageEntity findMessage(String messageId) { return messages.findById(messageId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Message not found")); }
    private void requireParticipant(String userId, MessageEntity message) { if (!userId.equals(message.senderId()) && !userId.equals(message.toUserId())) throw new ResponseStatusException(FORBIDDEN, "Message participant required"); }
    private String write(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException("Unable to serialize outbox event", exception); } }
}