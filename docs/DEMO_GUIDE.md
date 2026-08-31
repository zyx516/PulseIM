# PulseIM 演示指南

1. `docker compose up --build` 启动 MySQL、Redis、RabbitMQ、两个 IM 网关和业务服务；前端执行 `npm run dev`。
2. 浏览器 A 访问前端并设置 `VITE_IM_WS_URL=ws://localhost:8090/im/ws`，浏览器 B 设置为 `ws://localhost:8091/im/ws`；两个账号互发消息。
3. 点击已发送消息查看持久化、Outbox、MQ 发布及 ACK 轨迹。RabbitMQ 管理台 `http://localhost:15672` 可查看每个节点的 `.message-events` 队列及 `.dlq` 死信队列。
4. 断开其中一端并重连，历史消息由会话序号补拉；重复相同 `clientMessageId` 不新增消息。

## DLQ 规则

每个 Netty 节点都有独立业务队列与死信队列。消费异常且不再 requeue 时，RabbitMQ 将原始消息投递到 `pulseim.im.dlx`，再路由至该节点的 `.dlq`，供排查或受控重放。