## Context

Thunder Note 聊天模块当前已完成 P1（pending/outbox 主链）和 P2 部分（本地单一事实源起步），文本和媒体消息（IMAGE/VIDEO/FILE/VOICE）已通过 pending 管道实现离线排队和失败重试。但卡片消息（COMPOSITE）仍走旧的直发路径，本地消息缓存（`MessageLocalEntity`）缺少 `payload` 字段，多选模式只有合并没有删除，收集箱无法清空。

当前关键技术约束：
- Server 端 `messages` 表已有 `thumbnail_url`、`payload`（JSONB）字段
- Android Room DB 当前 version = 8，`MessageLocalEntity` 无 `payload` 字段
- `PendingMessage` 已有 `thumbnailUrl` 字段但从未填充
- `MessageAdapter.bindRetryState()` 只在 `showTextOnly()` 中调用

## Goals / Non-Goals

**Goals:**
- 卡片消息可在线/离线发送，且历史卡片正常显示
- 所有消息类型失败后统一显示重发图标
- 图片/视频发送前生成缩略图，聊天列表显示缩略图降低带宽
- 删除消息后保持聊天窗当前位置
- 多选模式支持批量删除（含 Server 端）
- 收集箱支持清空消息操作

**Non-Goals:**
- 不在此版本实现卡片消息的附件级别断点续传（卡片内附件上传失败则整条卡片标记失败，重试时重新上传全部附件）
- 不重构 `PendingMessageDispatcher` 为通用后台调度器
- 不实现服务端缩略图生成（仅 Android 端生成）
- 不改变合并消息的交互流程

## Decisions

### D1: 卡片消息纳入 pending 管道的策略

**决策**：将卡片消息视为一种特殊的 pending 类型 `COMPOSITE`，在 `PendingMessage` 中新增 `payloadJson` 字段存储序列化后的 `CardPayload`。`CardEditorFragment` 不再直接调用 `sendMessage()`，而是把包含本地附件路径的 payload 直接入队；`PendingMessageDispatcher` 负责逐个上传卡片附件/缩略图、回写 payload 中的远端 URL，然后再发送最终 `COMPOSITE` 消息。

**替代方案**：让 CardEditor 先上传完所有附件再 enqueue。这个方案虽然看起来更简单，但无法满足“卡片离线创建后继续发送”的要求，因为离线时连 pending 都无法创建。最终改为 dispatcher 负责附件上传，换取真正的离线入队能力。

**关键约束**：卡片 pending 的状态机改为 `QUEUED → UPLOADING → SENDING → SENT/FAILED`。卡片内附件上传与最终消息发送都在 dispatcher 中完成。

### D2: MessageLocalEntity 存储 payload 的方式

**决策**：在 `MessageLocalEntity` 新增 `payloadJson` 字段（String 类型），存储 CardPayload 的 JSON 序列化字符串。在 `toLocalMessage()` / `toMessageList()` 转换时用 Gson 反序列化为 `CardPayload`。

**理由**：Room 不原生支持复杂对象嵌套，用 JSON 字符串存储是最简单的方案，且与 Server 端的 JSONB 策略一致。Room DB version 升级到 9。

### D3: 缩略图生成策略

**决策**：
- 图片：用 `BitmapFactory` 加载后缩放到最大边 200px，保存为 JPEG（quality=70）
- 视频：用 `MediaMetadataRetriever` 提取第一帧，缩放到最大边 200px
- 缩略图作为独立文件上传到 MinIO，URL 存入 `PendingMessage.thumbnailUrl`
- `MessageAdapter.showImageMessage()` 改为优先加载 `thumbnailUrl`（有值时），点击后用全屏 viewer 加载原图 `mediaUrl`

**时机**：在 `MessageRepositoryImpl.enqueueMedia()` 中，创建 PendingMessage 之前同步生成缩略图文件。上传缩略图作为 dispatcher 的第一步（在上传原文件之前），这样缩略图 URL 可以尽早用于 pending 消息的占位显示。

### D4: 删除后保持滚动位置

**决策**：在 `ChatFragment` 中引入 `skipNextScroll` 布尔标志。调用 `deleteMessage()` 前设为 true；消息 observer 检查此标志，为 true 时跳过 `scrollToBottomAfterLayout()`，并在消费后重置。

### D5: 批量删除 API 设计

**决策**：Server 端新增 `POST /api/messages/delete-batch`，body 为 `{ "ids": [1, 2, 3] }`。使用 POST 而非 DELETE 是因为 DELETE 请求 body 在某些 HTTP 客户端中不被完全支持。上限 50 条。

### D6: 收集箱清空消息

**决策**：Server 端新增 `DELETE /api/messages/clear-inbox`。Android 端将收集箱的左滑删除行为改为弹出确认框"确定要清空收集箱所有消息吗？"，确认后调用此 API 删除 Server 端消息，同时清理本地 Room 中该会话的 confirmed 消息和 pending 消息。

## Risks / Trade-offs

- **[Risk] 卡片 pending 在附件上传中途失败** → 重试时 dispatcher 会继续从当前 payload 状态推进；已上传成功的 item 继续复用远端 URL，仍持有 `localPath` 的 item 才会继续上传。
- **[Risk] Room DB 升级可能导致数据丢失** → 当前仍使用 `fallbackToDestructiveMigration()`，本轮保持不变。后续 P2 全量铺开时统一迁移到正式 migration。
- **[Risk] 缩略图生成耗时阻塞 enqueue** → 缩略图生成在后台线程执行（enqueueMedia 已在 executor 中），不会阻塞 UI。但大图片/4K 视频首帧提取可能需要 200-500ms。
- **[Risk] 批量删除 50 条上限可能不够** → 与合并消息上限对齐（50 条），当前够用。
- **[Trade-off] 卡片 pending 不支持附件断点续传** → 降低了复杂度但用户重试卡片时会重新上传附件。实际影响小，因为卡片附件通常是图片/视频，已在上传前压缩过。
