# PulseIM group chat and delivery dashboard acceptance notes

This document records the user-facing behavior that must be true before the
current UX upgrade can be called complete. It intentionally separates planned
work from verified work, so the project does not describe unfinished features as
already shipped.

## Target scope

- Group chat must support creating a group, inviting members, opening a group
  conversation, sending text messages, receiving group messages, and reading
  group history.
- Group message fanout must be event driven: the message service persists one
  canonical group message, creates outbox events for target members, and lets
  RabbitMQ plus the delivery service handle asynchronous delivery.
- Delivery dashboard must show real message status data from persistence,
  outbox, MQ, routing, push, ack, and read events. It must not display static
  demo values.
- The web UI must expose these features as normal product workflows instead of
  hidden API-only capabilities.

## Required UX checks

- A logged-in user can create a group from the conversation/contact area.
- The group owner can invite a user by account or user id.
- The invited user can see or enter the group conversation after sync.
- Messages sent in a group are visible in group history after refresh.
- At mentions and reply preview fields are rendered without breaking older text
  messages.
- Empty, loading, failure, and reconnecting states are visually distinct.
- The delivery dashboard can be opened from the chat screen and scoped to the
  current user's real messages.

## Required backend checks

- Group member add/remove events are published through RabbitMQ.
- Conversation membership is synchronized from group member events.
- Duplicate group member events do not create duplicate conversation members.
- Duplicate clientMessageId values do not insert a second message.
- Duplicate MQ delivery events do not produce duplicate visible delivery state.
- Outbox failures record attempt count and last error.
- Events that exceed retry policy can be routed to a broker-level DLQ.

## Demo script

1. Start MySQL, Redis, RabbitMQ, the Spring services, two IM gateway nodes, and
   the web frontend.
2. Register or log in as user A and user B.
3. User A adds user B as a friend, and user B accepts.
4. User A creates a group and invites user B.
5. User A sends a group message with a reply preview or mention.
6. User B opens the group conversation and verifies the message appears.
7. Open the delivery dashboard and verify the displayed stages come from real
   persisted events.
8. Retry the same clientMessageId and verify there is still only one stored
   message.
9. Stop one IM gateway node, reconnect, and verify message history can be
   pulled after reconnection.

## Resume notes

The next implementation pass should first verify the current code state with:

```powershell
git status --short
rg -n "dashboard|GroupMemberChangedEvent|replyToMessageId|mentions|dlq|DeadLetter" -S
D:\maven3.9\bin\mvn.cmd --settings .mvn\settings.xml test
cd web-im
npm.cmd run build
```

If those commands pass, run the project locally and perform the demo script
above before marking the feature set complete.
