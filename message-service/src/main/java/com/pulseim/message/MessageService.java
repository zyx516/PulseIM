package com.pulseim.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseim.common.mq.MessagePersistedEvent;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;
    private final FanoutExchange exchange;
    private final ObjectMapper mapper;

    MessageService(MessageRepository messages, ConversationSequenceRepository sequences, OutboxEventRepository outboxEvents,
                   RabbitTemplate rabbitTemplate, FanoutExchange exchange, ObjectMapper mapper) {
        this.messages = messages;
        this.sequences = sequences;
        this.outboxEvents = outboxEvents;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.mapper = mapper;
    }

    @Transactional
    public MessageView persist(String senderId, MessageController.SendMessageCommand command) {
        return messages.findBySenderIdAndClientMessageId(senderId, command.clientMessageId())
                .map(MessageView::from)
                .orElseGet(() -> create(senderId, command));
    }

    private MessageView create(String senderId, MessageController.SendMessageCommand command) {
        try {
            ConversationSequenceEntity sequence = sequences.findByConversationId(command.conversationId())
                    .orElseGet(() -> sequences.saveAndFlush(new ConversationSequenceEntity(command.conversationId())));
            Instant now = Instant.now();
            MessageEntity message = new MessageEntity("m-" + UUID.randomUUID(), command.clientMessageId(),
                    command.conversationId(), senderId, command.toUserId(), command.content(), sequence.next(), now, "NORMAL");
            MessageEntity saved = messages.saveAndFlush(message);
            MessagePersistedEvent event = new MessagePersistedEvent("evt-" + UUID.randomUUID(), saved.id(),
                    saved.clientMessageId(), saved.conversationId(), saved.senderId(), saved.toUserId(),
                    saved.content(), saved.sequence(), saved.createdAt(), Instant.now());
            outboxEvents.save(new OutboxEventEntity(event.eventId(), "MESSAGE_PERSISTED", saved.id(), write(event), event.eventCreatedAt()));
            publish(event);
            return MessageView.from(saved);
        } catch (DataIntegrityViolationException duplicate) {
            return messages.findBySenderIdAndClientMessageId(senderId, command.clientMessageId())
                    .map(MessageView::from).orElseThrow(() -> duplicate);
        }
    }

    public List<MessageView> history(String conversationId, long afterSequence) {
        return messages.findTop50ByConversationIdAndSequenceGreaterThanOrderBySequenceAsc(conversationId, afterSequence)
                .stream().map(MessageView::from).toList();
    }

    @Transactional
    public MessageView recall(String userId, String messageId) {
        MessageEntity message = messages.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Message not found"));
        if (!message.senderId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "Only the sender can recall this message");
        }
        message.recall();
        return MessageView.from(message);
    }

    public List<OutboxEventView> outbox() {
        return outboxEvents.findTop100ByOrderByCreatedAtDesc().stream().map(OutboxEventView::from).toList();
    }

    private void publish(MessagePersistedEvent event) {
        try {
            rabbitTemplate.convertAndSend(exchange.getName(), "", event);
            outboxEvents.findById(event.eventId()).ifPresent(outbox -> {
                outbox.markPublished();
                outboxEvents.save(outbox);
            });
        } catch (AmqpException ignored) {
            // The persisted outbox row remains PENDING and can be retried after RabbitMQ is restored.
        }
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize outbox event", exception);
        }
    }
}
