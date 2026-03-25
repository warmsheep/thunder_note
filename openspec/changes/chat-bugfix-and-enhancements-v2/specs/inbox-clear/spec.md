## ADDED Requirements

### Requirement: Server 端清空收集箱消息 API
系统 SHALL 提供 `DELETE /api/messages/clear-inbox` 端点，删除当前用户收集箱（flashNoteId=-1）内的所有消息及关联媒体文件。

#### Scenario: 清空收集箱消息成功
- **WHEN** 用户调用清空收集箱 API
- **THEN** Server 删除所有 flashNoteId=-1 且 senderId=当前用户 的消息及其 MinIO 媒体文件，返回成功

#### Scenario: 收集箱无消息时
- **WHEN** 收集箱内没有消息
- **THEN** Server 返回成功（幂等操作）

### Requirement: 收集箱左滑改为清空消息
系统 SHALL 将 Android 端收集箱的左滑删除行为从"弹出不可删除提示"改为"弹出确认框清空消息"。

#### Scenario: 左滑收集箱弹出清空确认
- **WHEN** 用户在闪记列表中左滑收集箱
- **THEN** 弹出确认框"确定要清空收集箱所有消息吗？删除后不可恢复。"

#### Scenario: 确认清空后删除所有消息
- **WHEN** 用户在确认框中点击"清空"
- **THEN** 调用 Server 清空 API + 清除本地 Room 中收集箱对应的 confirmed 消息和 pending 消息，收集箱本身保留

#### Scenario: 取消清空操作
- **WHEN** 用户在确认框中点击"取消"
- **THEN** 不执行任何操作，收集箱状态不变
