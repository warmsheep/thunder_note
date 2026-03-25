## ADDED Requirements

### Requirement: MessageLocalEntity 持久化 payload
系统 SHALL 在 Android `MessageLocalEntity` 中新增 `payloadJson` 字段（String 类型），用于存储卡片消息的 CardPayload JSON。

#### Scenario: 保存卡片消息到本地时保留 payload
- **WHEN** 一条 COMPOSITE 消息从 Server 返回并存入本地 Room 数据库
- **THEN** `payloadJson` 字段包含完整的 CardPayload JSON 字符串

#### Scenario: 从本地加载卡片消息时恢复 payload
- **WHEN** 聊天页从本地 Room 加载 COMPOSITE 消息
- **THEN** 消息的 `payload` 字段通过 Gson 反序列化恢复为 `CardPayload` 对象，可正常渲染

### Requirement: 卡片消息通过 pending 管道发送
系统 SHALL 将卡片消息纳入 pending 管道。`CardEditorFragment` 保存时直接创建一条 `mediaType=COMPOSITE` 的 PendingMessage（包含带本地附件路径的 `payloadJson`），由 `PendingMessageDispatcher` 负责上传卡片附件/缩略图并最终发送。

#### Scenario: 在线发送卡片消息
- **WHEN** 用户在线编辑并保存卡片消息
- **THEN** CardEditorFragment 立即 enqueue 一条 COMPOSITE pending，dispatcher 上传附件后将其发送到 Server

#### Scenario: 离线发送卡片消息
- **WHEN** 用户在离线状态下保存卡片消息
- **THEN** 系统仍创建 COMPOSITE pending，本地聊天列表可立即显示卡片占位；dispatcher 在网络恢复后继续上传附件并发送

#### Scenario: 在线保存但发送阶段网络中断
- **WHEN** dispatcher 在上传附件或发送 COMPOSITE 消息阶段网络中断
- **THEN** pending 消息标记为 FAILED，用户可在聊天列表中看到失败重发图标，点击重试

### Requirement: PendingMessage 支持 payloadJson 字段
系统 SHALL 在 `PendingMessage` Room 实体中新增 `payloadJson` 字段，用于持久化卡片消息的 CardPayload。

#### Scenario: COMPOSITE pending 消息持久化 payload
- **WHEN** 一条 COMPOSITE 消息入队 pending
- **THEN** `payloadJson` 包含完整 CardPayload 的 JSON，dispatcher 发送时可反序列化
