## ADDED Requirements

### Requirement: 图片消息发送前生成缩略图
系统 SHALL 在 Android 端发送图片消息前，同步生成一张最大边不超过 200px 的 JPEG 缩略图文件。

#### Scenario: 发送图片时生成缩略图
- **WHEN** 用户选择图片发送
- **THEN** 系统在创建 PendingMessage 前生成缩略图文件，保存到应用缓存目录

### Requirement: 视频消息发送前提取首帧缩略图
系统 SHALL 在 Android 端发送视频消息前，使用 `MediaMetadataRetriever` 提取视频第一帧并缩放到最大边 200px 生成 JPEG 缩略图。

#### Scenario: 发送视频时提取首帧
- **WHEN** 用户选择视频发送
- **THEN** 系统提取第一帧作为缩略图，保存到应用缓存目录

### Requirement: 缩略图独立上传
系统 SHALL 将缩略图作为独立文件上传到 MinIO，获得的 URL 存入 `PendingMessage.thumbnailUrl`，并在发送消息时通过 `Message.thumbnailUrl` 传给 Server 端持久化。

#### Scenario: 缩略图上传成功后设置 URL
- **WHEN** PendingMessageDispatcher 开始处理 IMAGE/VIDEO 类型的 pending 消息
- **THEN** dispatcher 先上传缩略图文件获取 thumbnailUrl，再上传原文件获取 mediaUrl，两者都设入 Message 后发送

### Requirement: 聊天列表图片消息优先显示缩略图
系统 SHALL 在 `MessageAdapter.showImageMessage()` 中优先加载 `thumbnailUrl`（当非空时），不再直接加载全尺寸原图。

#### Scenario: 图片消息有缩略图时显示缩略图
- **WHEN** 聊天列表渲染一条 IMAGE 消息且 `thumbnailUrl` 非空
- **THEN** 使用 Glide 加载 `thumbnailUrl` 显示

#### Scenario: 图片消息无缩略图时降级显示原图
- **WHEN** 聊天列表渲染一条 IMAGE 消息且 `thumbnailUrl` 为空
- **THEN** 使用 Glide 加载 `mediaUrl` 显示（兼容历史消息）

### Requirement: 点击图片查看原图
系统 SHALL 在用户点击图片消息时，打开全屏图片查看器加载 `mediaUrl` 原图。

#### Scenario: 点击缩略图打开原图
- **WHEN** 用户点击聊天列表中的图片消息
- **THEN** 打开全屏 viewer 加载 `mediaUrl`（非缩略图）
