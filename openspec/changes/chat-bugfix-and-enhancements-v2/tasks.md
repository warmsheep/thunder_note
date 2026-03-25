## 1. 基础设施：数据库 & 模型层变更

- [x] 1.1 Android `MessageLocalEntity` 新增 `payloadJson` 字段（String），Room DB version 升级到 9
- [x] 1.2 Android `PendingMessage` 新增 `payloadJson` 字段（String），更新 DAO 查询
- [x] 1.3 Android `MessageRepositoryImpl.toLocalMessage()` 增加 payload → payloadJson 序列化写入
- [x] 1.4 Android `MessageRepositoryImpl.toMessageList()` 增加 payloadJson → payload 反序列化读取
- [x] 1.5 Server 端 `MessageServiceImpl.sendMessage()` 的 content fallback switch 补充 `COMPOSITE` case

## 2. BUG 修复：媒体消息失败重发图标

- [x] 2.1 `MessageAdapter.showImageMessage()` 末尾添加 `bindRetryState(holder, message, mine)`
- [x] 2.2 `MessageAdapter.showVideoMessage()` 末尾添加 `bindRetryState(holder, message, mine)`
- [x] 2.3 `MessageAdapter.showVoiceMessage()` 末尾添加 `bindRetryState(holder, message, mine)`
- [x] 2.4 `MessageAdapter.showFileMessage()` 末尾添加 `bindRetryState(holder, message, mine)`
- [x] 2.5 `MessageAdapter.showCompositeMessage()` 末尾添加 `bindRetryState(holder, message, mine)`

## 3. BUG 修复：删除消息后保持滚动位置

- [x] 3.1 `ChatFragment` 新增 `skipNextScroll` 布尔标志字段
- [x] 3.2 `ChatFragment.handleDelete()` 在调用 deleteMessage 前设置 `skipNextScroll = true`
- [x] 3.3 `ChatFragment` 消息 observer 中检查 `skipNextScroll`，为 true 时跳过 `scrollToBottomAfterLayout()` 并重置标志

## 4. 功能：卡片消息纳入 pending 管道

- [x] 4.1 `MessageRepositoryImpl` 新增 `enqueueCompositeMessage(conversationKey, flashNoteId, peerUserId, Message)` 方法，创建 COMPOSITE 类型 PendingMessage 并 dispatch
- [x] 4.2 `PendingMessageDispatcher` 扩展 dispatch 逻辑：COMPOSITE 类型跳过 upload 阶段，直接进入 SENDING
- [x] 4.3 `PendingMessageDispatcher.sendPendingMessage()` 处理 COMPOSITE 时从 `payloadJson` 反序列化 payload 设入 Message
- [x] 4.4 `CardEditorFragment.sendCompositeMessage()` 改为调用 `messageRepository.enqueueCompositeMessage()` 替代直接调用 `sendMessage()`
- [ ] 4.5 [blocked: 缺少真实联机聊天运行环境] 验证：在线发送卡片 + 网络中断后重试卡片 + 历史卡片正常显示

## 5. 功能：图片/视频缩略图

- [x] 5.1 新建 `ThumbnailUtils` 工具类：`generateImageThumbnail(File) → File`（最大边 200px JPEG70）和 `generateVideoThumbnail(File) → File`（首帧 200px JPEG70）
- [x] 5.2 `PendingMessageDispatcher` 在上传原文件前先生成并上传缩略图，设入 `PendingMessage.thumbnailUrl`
- [x] 5.3 `PendingMessageDispatcher.sendPendingMessage()` 将 thumbnailUrl 设入 Message 发给 Server
- [x] 5.4 `MessageAdapter.showImageMessage()` 改为优先加载 `thumbnailUrl`（非空时），点击打开原图
- [ ] 5.5 [blocked: 缺少真实联机聊天运行环境] 验证：发送图片/视频后 Server 端 messages 表 thumbnail_url 有值；聊天列表显示缩略图

## 6. 功能：多选批量删除

- [x] 6.1 Server 端 `MessageController` 新增 `POST /api/messages/delete-batch` 端点
- [x] 6.2 Server 端 `MessageService/Impl` 新增 `deleteMessages(username, List<Long> ids)` 方法（含权限校验 + 媒体文件清理）
- [x] 6.3 Android `MessageApi` 新增 `deleteBatch(@Body BatchDeleteRequest)` 接口声明
- [x] 6.4 `fragment_chat.xml` 的 `mergePanel` 新增"删除"按钮（在"合并"按钮旁边）
- [x] 6.5 `ChatFragment` 新增 `handleBatchDelete()` 方法：收集选中 ID → 确认框 → 调用 API → 清理本地 → 退出多选
- [x] 6.6 `ChatViewModel` 新增 `deleteMessages(List<Long> ids, Callback)` 方法
- [x] 6.7 `MessageRepositoryImpl` 新增 `deleteMessages(List<Long> ids, Callback)` 方法（调 API + 删本地 Room）
- [x] 6.8 批量删除也触发 `skipNextScroll` 标志

## 7. 功能：收集箱清空消息

- [x] 7.1 Server 端 `MessageController` 新增 `DELETE /api/messages/clear-inbox` 端点
- [x] 7.2 Server 端 `MessageServiceImpl` 新增 `clearInboxMessages(username)` 方法（删消息 + 清媒体）
- [x] 7.3 Android `MessageApi` 新增 `clearInbox()` 接口声明
- [x] 7.4 `FlashNoteTabFragment.onDelete()` 中收集箱分支改为弹出"清空消息"确认框
- [x] 7.5 `FlashNoteViewModel` / `FlashNoteRepository` 新增 `clearInboxMessages(Callback)` 方法
- [x] 7.6 清空成功后同步清理本地 Room 中收集箱的 confirmed 消息和 pending 消息

## 8. 构建验证 & 文档同步

- [x] 8.1 `cd thunder-note-server && mvn clean compile` 通过
- [x] 8.2 `cd thunder-note-server && mvn test` 通过
- [x] 8.3 `cd thunder-note-android && ./gradlew assembleDebug` 通过
- [x] 8.4 `cd thunder-note-android && ./gradlew test` 通过
- [x] 8.5 更新 `docs/完整开发计划.md` 中相关任务状态
- [x] 8.6 更新 `docs/数据库设计.md`（如有 schema 变更）
- [x] 8.7 更新 `docs/API接口设计.md`（新增批量删除 + 清空收集箱 API）
- [x] 8.8 更新 `docs/开发测试部署经验库.md`（本轮遇到的经验）
- [x] 8.9 代码审查：确认所有变更符合项目规范
