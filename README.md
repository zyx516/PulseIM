# PulseIM

PulseIM 是一个面向毕业设计/求职展示的分布式即时通信系统。项目重点展示微服务拆分、Netty WebSocket 长连接、消息持久化、RabbitMQ 事件投递、Redis 在线状态、多端同步预留和可观测性基础。

> 当前项目处于演示实现阶段，不声明未经实测的 QPS、并发连接数或延迟指标。

## 技术栈

- 后端：Java 17、Spring Boot 3.2、Spring Cloud、Spring Cloud Gateway、OpenFeign
- 长连接：Netty、WebSocket
- 存储：MySQL、Redis
- 消息队列：RabbitMQ
- 前端：React、TypeScript、Vite、Ant Design、TanStack Query、Zustand
- 可观测性：Micrometer、Prometheus、Grafana
- 本地编排：Docker Compose

## 模块说明

| 模块 | 端口 | 职责 |
| --- | ---: | --- |
| `api-gateway` | 8080 | HTTP 统一入口和路由 |
| `auth-service` | 8081 | 注册、登录、JWT |
| `user-service` | 8082 | 用户资料 |
| `social-service` | 8083 | 好友关系、群组和成员 |
| `conversation-service` | 8084 | 会话、已读游标、置顶和免打扰 |
| `message-service` | 8085 | 消息持久化、幂等、会话序号、Outbox 和 RabbitMQ 事件 |
| `media-service` | 8086 | 图片/文件元数据和上传凭证占位 |
| `moderation-service` | 8087 | 敏感内容审核事件记录 |
| `im-gateway` | 8090 | Netty WebSocket 长连接、鉴权、心跳和实时推送 |
| `web-im` | 5173 | Web IM 客户端 |

## 已实现能力

- 用户注册、登录、JWT access/refresh token 基线
- 好友申请、同意、删除
- 单聊会话、群组和成员基础模型
- 文本消息发送、历史消息查询、会话内递增序号
- `clientMessageId` 幂等写入，避免重复发送重复入库
- MySQL 持久化，服务重启后业务数据不依赖内存 Map
- Outbox 事件表和 RabbitMQ `MESSAGE_PERSISTED` 事件
- Redis TTL 在线状态模型：`user/device -> node`
- Netty WebSocket 首帧 `AUTH` 鉴权、心跳和消息推送
- 前端三栏 IM 布局、登录注册、会话、联系人和消息页

## 本地启动

### 1. 构建后端

```powershell
D:\maven3.9\bin\mvn.cmd --settings .mvn\settings.xml -DskipTests package
```

### 2. 使用 Docker Compose 启动全套服务

```powershell
docker compose up --build
```

Docker Compose 会启动 MySQL、Redis、RabbitMQ、Nacos、各个后端服务和两个 `im-gateway` 节点。

### 3. 连接本机已有 MySQL/Redis/RabbitMQ

如果你不使用 Docker Compose，而是连接本机中间件，可以使用脚本：

```powershell
.\scripts\start-local.ps1 -MysqlUser root -MysqlPassword 你的MySQL密码
```

停止本地进程：

```powershell
.\scripts\stop-local.ps1
```

### 4. 启动前端

```powershell
cd web-im
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

默认演示账号：

- `pulse / pulse123`
- `ava / ava123`

## WebSocket 协议示例

连接地址：

```text
ws://localhost:8090/im/ws
```

连接建立后，客户端必须先发送 `AUTH` 帧：

```json
{
  "version": "1",
  "requestId": "uuid",
  "command": "AUTH",
  "data": {
    "token": "<access-token>"
  }
}
```

核心命令包括：

- `AUTH`
- `PING`
- `SEND_MESSAGE`
- `ACK`
- `READ`
- `MESSAGE_EVENT`

## 验证命令

```powershell
D:\maven3.9\bin\mvn.cmd --settings .mvn\settings.xml test
cd web-im
npm run build
```

## 设计文档

阶段二、阶段三新增能力和问题修复说明见：

[docs/PHASE_2_3_DELIVERY.md](docs/PHASE_2_3_DELIVERY.md)

## 安全说明

仓库中的默认密码和 JWT secret 仅用于本地演示。部署到任何真实环境前，请通过环境变量替换：

- `PULSEIM_MYSQL_PASSWORD`
- `RABBITMQ_PASSWORD`
- `PULSEIM_JWT_SECRET`

不要提交真实 `.env`、日志、数据库 dump、私钥或线上用户数据。
# PulseIM
