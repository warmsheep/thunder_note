## ADDED Requirements

### Requirement: Server 端批量删除消息 API
系统 SHALL 提供 `POST /api/messages/delete-batch` 端点，接受 `{ "ids": [Long] }` 请求体，一次最多删除 50 条消息。

#### Scenario: 批量删除成功
- **WHEN** 用户发送包含 1-50 个有效消息 ID 的批量删除请求
- **THEN** Server 删除所有指定消息及其关联的媒体文件（MinIO），返回成功

#### Scenario: 超过 50 条上限
- **WHEN** 请求包含超过 50 个消息 ID
- **THEN** Server 返回 400 BAD_REQUEST 错误

#### Scenario: 部分消息不属于当前用户
- **WHEN** 请求中包含不属于当前用户的消息 ID
- **THEN** Server 返回 403 FORBIDDEN 错误，不删除任何消息

### Requirement: 多选模式显示批量删除按钮
系统 SHALL 在聊天页多选模式的合并面板中，新增一个"删除"按钮，与"合并"按钮并列。

#### Scenario: 进入多选模式时显示删除按钮
- **WHEN** 用户长按消息选择"多选"进入多选模式
- **THEN** 底部面板同时显示"取消"、"合并"、"删除"三个按钮

### Requirement: 批量删除选中消息
系统 SHALL 支持用户在多选模式下点击"删除"按钮批量删除选中的消息。

#### Scenario: 选中消息后点击删除
- **WHEN** 用户选中 1 条或多条消息后点击"删除"按钮
- **THEN** 弹出确认框"确定要删除选中的 N 条消息吗？"，确认后调用批量删除 API

#### Scenario: 批量删除后更新本地
- **WHEN** Server 端批量删除成功
- **THEN** 同步从本地 Room 数据库删除对应消息，列表即时更新，保持当前滚动位置
