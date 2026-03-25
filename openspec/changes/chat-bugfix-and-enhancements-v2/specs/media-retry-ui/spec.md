## ADDED Requirements

### Requirement: 所有消息类型失败时显示重发图标
系统 SHALL 对所有消息类型（TEXT/IMAGE/VIDEO/FILE/VOICE/COMPOSITE）在发送失败时统一显示重发图标。

#### Scenario: 图片消息发送失败显示重发图标
- **WHEN** 一条 IMAGE 消息处于 FAILED 状态（ID 为负数且非上传中）
- **THEN** `MessageAdapter` 在该消息右侧显示重发图标，点击可触发重试

#### Scenario: 视频消息发送失败显示重发图标
- **WHEN** 一条 VIDEO 消息处于 FAILED 状态
- **THEN** 显示重发图标，点击可触发重试

#### Scenario: 文件消息发送失败显示重发图标
- **WHEN** 一条 FILE 消息处于 FAILED 状态
- **THEN** 显示重发图标，点击可触发重试

#### Scenario: 语音消息发送失败显示重发图标
- **WHEN** 一条 VOICE 消息处于 FAILED 状态
- **THEN** 显示重发图标，点击可触发重试

#### Scenario: 卡片消息发送失败显示重发图标
- **WHEN** 一条 COMPOSITE 消息处于 FAILED 状态
- **THEN** 显示重发图标，点击可触发重试

#### Scenario: 上传中的消息不显示重发图标
- **WHEN** 一条媒体消息处于 UPLOADING 状态
- **THEN** 不显示重发图标（上传仍在进行中）
