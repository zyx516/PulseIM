# PulseIM 阶段二/三交付说明

## 新增能力

- MySQL 持久化：`auth-service`、`user-service`、`social-service`、`conversation-service`、`message-service`、`media-service`、`moderation-service` 都已从内存模型切到 JPA/MySQL。
- 消息幂等：`message-service` 在 `im_messages` 上增加 `sender_id + client_message_id` 唯一约束，重复发送同一个 `clientMessageId` 会返回同一条服务端消息，不重复入库。
- 会话内顺序号：`conversation_sequences` 按会话维护递增序号，消息表再用 `conversation_id + sequence` 唯一约束保护顺序。
- RabbitMQ 消息事件：消息落库后写入 `outbox_events`，再发布 `MESSAGE_PERSISTED` 到 `pulseim.message.events` fanout exchange。
- Outbox 重试：RabbitMQ 暂时不可用时，Outbox 保留 `PENDING` 记录，定时任务恢复发布。
- Redis 在线状态：`im-gateway` 在 WebSocket `AUTH`、`PING`、断线时写入或删除 `pulseim:presence:{userId}:{deviceId}`，TTL 默认 75 秒。
- 多节点推送模型：每个 `im-gateway` 使用独立 RabbitMQ 队列订阅消息事件，只向本节点在线连接推送，避免业务服务持有连接状态。
- 群聊基础：`social-service` 增加 `/api/groups`、群成员、角色；`conversation-service` 增加群会话入口。
- 会话状态：`conversation-service` 增加已读游标、未读数、置顶、免打扰字段。
- 消息撤回：`message-service` 增加 `/api/messages/{messageId}/recall`，仅发送者可撤回。
- 媒体服务：新增 `media-service`，提供上传凭证占位和媒体元数据持久化。
- 审核服务：新增 `moderation-service`，提供文本审核检查和审核事件持久化。

## 重点问题与解决方式

- 重复发送：用数据库唯一索引解决，而不是只靠内存 Set。这样服务重启后幂等仍然有效。
- MQ 至少一次投递：消费端本地按 `messageId` 去重，消息服务用 Outbox 记录发布状态，避免“落库成功但事件完全丢失”。
- 未读重复累加：会话未读采用 `latestSequence - readSequence` 计算，不按设备或推送次数累加。
- 在线状态漂移：Redis presence key 带 TTL，心跳刷新；断开连接会主动删除，异常断开可由 TTL 自然过期。
- 跨节点连接隔离：Netty 只维护本节点 Channel，RabbitMQ fanout 负责把“消息已持久化”事件广播到各节点。
- 数据所有权：每个服务独立数据库，避免多个服务直接写同一张业务表。

## 使用的技术

- Spring Boot 3.2.5
- Spring Data JPA / Hibernate
- MySQL 8.x
- RabbitMQ fanout exchange + Spring AMQP
- Redis + Spring Data Redis
- Netty WebSocket
- JWT
- Docker Compose
- React + TypeScript + Vite + Ant Design

## 本地运行提醒

当前代码默认 MySQL 账号为 `root`，密码为 `pulseim-dev`。如果你的本地 MySQL 不是这个密码，需要启动前传入：

```powershell
.\scripts\start-local.ps1 -MysqlUser root -MysqlPassword 你的密码
```

也可以使用环境变量：

```powershell
$env:PULSEIM_MYSQL_USER="root"
$env:PULSEIM_MYSQL_PASSWORD="你的密码"
```

RabbitMQ 默认使用 `guest/guest`，Redis 默认使用 `localhost:6379`。

## 验证结果

- `mvn test` 已通过，包含 JWT 测试和消息幂等持久化测试。
- `mvn -DskipTests package` 已通过，所有 11 个 Maven 模块均生成可执行 jar。
- 本机启动验证发现 MySQL `root` 默认密码不匹配，服务日志报 `Access denied for user 'root'@'localhost'`；需要用你的真实 MySQL 密码启动。
- 本机 Redis `6379` 当前未连通；Redis 代码已接入，服务启动后会在可用时写 presence key。
