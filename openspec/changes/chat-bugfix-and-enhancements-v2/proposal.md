## Why

聊天模块当前存在多个阻塞性 BUG 和体验缺陷：卡片消息在线/离线均无法发送且历史卡片显示"未知的卡片内容"；媒体消息（图片/视频/文件/语音/卡片）发送失败后无重发图标，用户无法感知失败状态；图片/视频缺少缩略图导致聊天列表加载慢且带宽浪费；删除消息后聊天窗跳到最底部；多选模式只能合并不能批量删除；收集箱左滑删除只弹"不可删除"但用户需要清空消息的能力。这些问题直接影响核心使用体验，需要在当前阶段集中修复。

## What Changes

- **BUG 修复**：修复卡片消息发送失败（在线时 `MessageLocalEntity` 缺少 `payload` 字段导致从本地加载时 payload 为 null；卡片消息未接入 pending 管道导致离线不支持）
- **BUG 修复**：修复删除消息后 `ChatFragment` 无条件滚动到底部的问题（消息 observer 对所有数据变化都调用 `scrollToBottomAfterLayout`）
- **UI 补齐**：为所有媒体消息类型（IMAGE/VIDEO/FILE/VOICE/COMPOSITE）补齐失败重发图标（当前只有 TEXT 调用了 `bindRetryState`）
- **功能新增**：发送图片/视频前在 Android 端生成缩略图，上传到 MinIO，聊天列表改为显示缩略图，点击后查看原图
- **功能新增**：多选模式新增批量删除按钮，支持一次删除多条消息（需新增 Server 端批量删除 API）
- **功能修改**：收集箱左滑不再弹"不可删除"，改为弹出确认框清空收集箱内所有消息（本地 + Server 端），保留收集箱本身

## Capabilities

### New Capabilities
- `media-thumbnail`: 图片/视频消息发送前生成缩略图、独立上传、聊天列表缩略图展示、点击查看原图
- `batch-message-delete`: 多选模式下的批量消息删除能力（Android UI + Server API）
- `inbox-clear`: 收集箱清空消息能力（保留收集箱实体，删除所有消息）

### Modified Capabilities
- `card-message-send`: 修复卡片消息本地持久化（`MessageLocalEntity` 增加 `payload` 字段）+ 将卡片消息纳入 pending 管道支持离线发送/重试
- `media-retry-ui`: 所有消息类型统一失败重发图标显示
- `chat-scroll-behavior`: 删除消息后保持聊天窗原位置

## Impact

- **Server 端**：
  - `MessageController` 新增批量删除端点 `DELETE /api/messages/batch`
  - `MessageService/Impl` 新增 `deleteMessages(username, List<Long> ids)` + `clearInboxMessages(username)`
  - `FlashNoteController/Service` 新增清空收集箱消息端点
  - 无需 DB migration（`thumbnail_url`、`payload` 字段已存在）
- **Android 端**：
  - `MessageLocalEntity` 新增 `payload` 字段（JSON 字符串存储）→ Room DB schema version 升级
  - `MessageAdapter` 所有媒体 show 方法补 `bindRetryState` 调用
  - `ChatFragment` 消息 observer 增加删除标志跳过滚动
  - `ChatFragment` 多选面板新增删除按钮 + 批量删除逻辑
  - `FlashNoteTabFragment` 收集箱左滑行为改为清空消息
  - `PendingMessageDispatcher` 新增 COMPOSITE 类型的 pending 处理链路
  - 新增 `ThumbnailUtils` 工具类（图片缩放 + 视频首帧提取）
  - `MessageRepositoryImpl.enqueueMedia()` 增加缩略图生成 + 上传步骤
  - `fragment_chat.xml` 多选面板布局新增删除按钮
