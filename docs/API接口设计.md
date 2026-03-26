# API 接口设计

## 文档定位
本文档优先记录**当前已实现接口**，并单独说明**目标态未实现接口**。当前接口事实以后端 controller 为准。

## 当前统一约定
- 当前真实前缀：`/api/...`
- 不是 `/api/v1/...`
- 大多数普通 JSON 接口使用统一响应结构：`{ code, message, data, timestamp }`
- 当前登录请求字段使用 `username`，不是 `email`
- 当前主线 JSON 读取接口统一收敛为 `POST`；文件下载与 SSE 仍保留 `GET`

例外：
- `GET /api/messages/stream` 返回 `SseEmitter`，不是 `ApiResponse`
- `GET /api/files/download` 返回文件二进制响应，不是 `ApiResponse`

## 响应包装结构
```json
{
  "code": 0,
  "message": "Success",
  "data": {},
  "timestamp": "2026-03-13T00:00:00Z"
}
```

## 错误码约定
| code | 含义 | 来源 |
|------|------|------|
| 0 | 成功 | `ErrorCode.SUCCESS` |
| 40000 | 请求错误 | `ErrorCode.BAD_REQUEST` |
| 40100 | 未授权/认证失败 | `ErrorCode.UNAUTHORIZED` |
| 40300 | 禁止访问 | `ErrorCode.FORBIDDEN` |
| 40400 | 资源不存在 | `ErrorCode.NOT_FOUND` |
| 50000 | 服务端错误 | `ErrorCode.INTERNAL_ERROR` |

说明：
- 当前项目使用自定义 `ApiResponse`，不是 RFC Problem Details。
- 如果未来要切换到标准化错误响应，需要单独设计兼容与迁移方案，避免破坏现有 Android 解析逻辑。

## 当前请求/响应细节
### 认证

#### POST `/api/auth/login`
请求体：
```json
{
  "username": "alice",
  "password": "secret123"
}
```

成功响应 `data`：
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "user": {
    "id": 1,
    "username": "alice",
    "email": "alice@example.com",
    "nickname": "alice",
    "avatar": null
  }
}
```

校验规则：
- `username`：必填
- `password`：必填

#### POST `/api/auth/register`
请求体：
```json
{
  "username": "alice",
  "email": "alice@example.com",
  "password": "secret123"
}
```

校验规则：
- `username`：必填，长度 3-32
- `email`：必填，必须符合邮箱格式
- `password`：必填，长度 6-128

成功响应：`data = null`

#### POST `/api/auth/refresh`
- 当前真实实现已改为 **JSON body**，不再通过 URL query 传 refresh token
- 请求体：
```json
{
  "refreshToken": "refresh-token-string"
}
```
- 成功返回结构与登录相同
- 当前后端会校验“绝对会话时长”上限（30天）；超过上限需重新登录

#### POST `/api/auth/logout`
- 当前通过请求头中的 `Authorization` 退出登录
- 成功响应：`data = null`

### 用户

#### POST `/api/users/profile`
- 需要认证
- 成功响应 `data` 为 `UserProfile`

#### PUT `/api/users/profile`
- 需要认证
- 当前请求体直接提交 `UserProfile` 对象
- 当前支持更新 `bio`，并支持通过 `nickname` / `avatar` 回写到 `users` 表对应字段

#### GET `/api/users/contacts`
- 需要认证
- 返回联系人列表，并额外包含“我已发起、等待对方同意”的挂起联系人（`friend_relations.status=PENDING` 且 `requesterId=当前用户`）
- 响应 `data` 为 `List<ContactUserDto>`，字段：`userId`、`username`、`nickname`、`avatar`、`relationStatus`、`latestMessage`
- `relationStatus` 当前取值：`FRIEND` / `PENDING_SENT` / `PENDING_RECEIVED`
- `latestMessage` 为当前联系人会话最新一条消息预览；媒体类型统一返回 `[图片]/[视频]/[语音]/[文件]` 占位

#### GET `/api/users/contacts/requests`
- 需要认证
- 返回当前用户收到的待处理好友请求列表

#### GET `/api/users/contacts/requests/count`
- 需要认证
- 返回当前用户待处理好友请求数

#### POST `/api/users/contacts/request`
- 需要认证
- 请求体：`{ "targetUserId": 123 }`
- 发起好友请求（幂等：已是好友/已发请求时不重复创建）

#### POST `/api/users/contacts/request/accept`
- 需要认证
- 请求体：`{ "requestId": 456 }`
- 接受好友请求

#### POST `/api/users/contacts/request/reject`
- 需要认证
- 请求体：`{ "requestId": 456 }`
- 拒绝好友请求（接收方操作，将关系标记为 REJECTED）

#### DELETE `/api/users/contacts/request/{requestId}`
- 需要认证
- 取消已发送的好友申请（发送方操作，直接删除 pending 记录）

#### DELETE `/api/users/contacts/{contactUserId}`
- 需要认证
- 删除联系人关系或取消好友申请（对 FRIEND / PENDING 状态均生效，幂等）

#### GET `/api/users/contacts/search?keyword=...`
- 需要认证
- 搜索用户并返回关系状态：`NONE` / `FRIEND` / `PENDING_SENT` / `PENDING_RECEIVED`

### 闪记

#### POST `/api/flash-notes/list`
- 需要认证
- 返回当前用户的闪记列表
- 当前列表首位固定返回“收集箱”虚拟闪记（`id=-1`，默认置顶）
- 当前响应中的 `FlashNote` 额外返回 `latestMessage`（后端按该闪记最新一条消息聚合；媒体类型返回 `[图片]/[视频]/[语音]/[文件]` 占位）
- 当前响应中的 `FlashNote` 包含扩展状态：`pinned` / `hidden` / `inbox`

#### POST `/api/flash-notes/search`
- 需要认证
- 搜索闪记及其关联消息，返回两个独立列表避免重复
- 请求体：
```json
{
  "query": "搜索关键词"
}
```
- 响应 `data` 为 `FlashNoteSearchResponse`，包含两个独立列表：
  - `noteNameMatched`（`List<FlashNoteSearchResult>`）：命中闪记标题/正文的搜索结果
    - 每个结果包含：
      - `flashNote`：匹配的闪记对象
      - `noteMatched`：始终为 `true`
      - `matchedMessages`：匹配的消息列表（`List<MatchedMessageInfo>`），每个包含：
        - `messageId`：消息ID
        - `snippet`：消息内容摘要
  - `messageContentMatched`（`List<FlashNoteSearchResult>`）：仅命中消息内容的搜索结果
    - 每个结果包含：
      - `flashNote`：匹配的闪记对象
      - `noteMatched`：始终为 `false`
      - `matchedMessages`：匹配的消息列表（`List<MatchedMessageInfo>`）

#### POST `/api/flash-notes`
- 需要认证
- 当前请求体直接提交 `FlashNote` 实体形态
- 当前 `FlashNote` 额外包含 `icon` 字段；Android 当前创建流只要求图标 + 名称，`content` 允许为空
- 当前 MVP 约定 `tags` 承接单一合集/分类名称

#### PUT `/api/flash-notes/{id}`
- 需要认证
- 当前请求体直接提交 `FlashNote` 实体形态
- 当前 Android 编辑闪记时，同步更新图标、名称与合集归属
- 当前后端支持将 `tags` 更新为 `null`（用于“移出合集/未分类”）
- `id=-1`（收集箱）不允许编辑

#### PUT `/api/flash-notes/{id}/pin?value=true|false`
- 需要认证
- 设置或取消置顶
- `id=-1`（收集箱）不允许取消置顶

#### PUT `/api/flash-notes/{id}/hide?value=true|false`
- 需要认证
- 设置或取消隐藏
- 当 `value=true` 时会自动取消置顶
- `id=-1`（收集箱）不允许隐藏

#### DELETE `/api/flash-notes/{id}`
- 需要认证
- 当前为删除接口，不是隐藏接口

### 消息

#### POST `/api/messages/list`
- 需要认证
- 当前支持通过请求体可选字段 `flashNoteId` 拉取某条闪记下的消息列表
- 当前支持通过请求体可选字段 `peerUserId` 拉取与某个联系人之间的双向会话列表
- 当同时提供 `flashNoteId` 与 `peerUserId` 时，优先按 `flashNoteId` 过滤
- 当前支持 `flashNoteId=-1` 拉取“收集箱”消息
- 当 `flashNoteId=-1`（收集箱）时，后端会额外约束 `senderId=当前用户` 且 `receiverId=当前用户`，避免跨用户串读

#### POST `/api/messages`
- 需要认证
- 当前请求体直接提交 `Message` 实体形态
- 支持发送普通消息及复合结构消息(如：多图文/多视频卡片)
- 当前后端消息模型核心字段是 `receiverId`、`content`，并已补 `flashNoteId`、`role` 用于 Android 闪记内对话
- 当前联系人会话发送时，`flashNoteId` 可为空，`receiverId` 指向联系人用户ID
- 当前支持发送到 `flashNoteId=-1`（收集箱）
- 当前复合卡片消息可直接通过该接口发送：`mediaType=COMPOSITE`，`content/fileName` 承接卡片标题，`payload` 承接卡片结构（标题、summary、items）
- 当前 Android 端已通过独立卡片编辑页生成三类卡片：单/多图片+文字、单/多视频+文字、单/多文件+文字；同一卡片内附件需为同一类型，最多 9 个
- 当前 Android 聊天页成功发送后才清空输入框；失败时保留输入并提示错误

#### POST `/api/messages/merge`
- 需要认证
- 当前已实现：用于将同一闪记或同一联系人会话中的多条消息打包合并成一条复合卡片消息
- 请求体：
```json
{
  "title": "卡片标题（必填）",
  "messageIds": [1, 2, 3],
  "flashNoteId": 123,
  "receiverId": 456
}
```
- 约束：
  - `title` 必填
  - `messageIds` 不能为空，且最多 50 条
  - `flashNoteId` / `receiverId` 至少提供一个
  - 所选消息必须属于当前用户可访问的同一会话
- 后端逻辑：根据 `messageIds` 查出所有原消息内容，按请求顺序组装成深拷贝 JSON 结构，并生成一条新的 `mediaType=COMPOSITE` 消息存入数据库；当前 `payload` 会按 PostgreSQL `jsonb` 正确写入。
- 当前默认行为：生成一条新的卡片消息，原消息保留，不做删除。

#### GET `/api/messages/stream`
- 需要认证
- 当前使用 SSE
- 用于接收服务端推送的消息事件

#### DELETE `/api/messages/{id}`
- 需要认证
- 删除指定ID的消息
- 只能删除自己发送的消息或收到的消息
- 删除时会同步清理该消息关联的 `mediaUrl` / `thumbnailUrl`，以及复合卡片中 `originalMsgId == null` 的独立媒体对象
- 返回 `data = null`

#### POST `/api/messages/delete-batch`
- 需要认证
- 当前已实现：批量删除消息
- 请求体：
```json
{
  "ids": [1, 2, 3]
}
```
- 约束：
  - `ids` 不能为空
  - 一次最多 50 条
  - 所有消息都必须属于当前用户可访问的会话，否则整批拒绝
- 删除成功后会同步清理关联媒体对象

#### DELETE `/api/messages/clear-inbox`
- 需要认证
- 当前已实现：清空收集箱（`flashNoteId=-1`）里的所有消息，但**不删除收集箱实体本身**
- 服务端会额外约束 `senderId=当前用户` 且 `receiverId=当前用户`
- 删除成功后会同步清理收集箱消息及其关联媒体对象

#### GET `/api/messages/count`
- 需要认证
- 返回当前用户所有消息的总数
- 返回 `data` 为 `Long` 类型

### 合集

#### POST `/api/collections/list`
- 需要认证
- 返回当前用户的合集列表
- 当前 Android/原型把合集视为“分类目录”；默认展示时会结合 `flash_notes.tags` 组织为“合集 - 闪记”层级

#### POST / PUT / DELETE `/api/collections`
- 需要认证
- 当前请求体直接提交 `Collection` 实体形态
- 当前 `Collection.description` 仍保留在后端实体中，但 Android 主流程不再把“标题 + 描述”作为合集创建入口
- 当前后端在重命名合集时，会同步改写使用该合集名的 `flash_notes.tags`；删除合集时，会把这些闪记归属清空为未分类

### 文件

#### POST `/api/files/upload`
- 需要认证
- 上传方式：`multipart/form-data`
- 表单字段：`file`
- 当前服务端配置大小限制：单文件 500MB，请求总大小 500MB
- 当前 Android 图片/视频发送链路会额外上传一份缩略图文件，并将对象名写入 `messages.thumbnailUrl`

#### GET `/api/files/download?objectName=...`
- 当前以 `objectName` 查询参数指定文件对象

### 收藏

#### POST `/api/favorites/list`
- 需要认证
- 当前返回当前用户的已收藏消息列表
- 当收藏消息属于收集箱（`flashNoteId=-1`）时，当前会额外返回 `flashNoteTitle=收集箱`、`flashNoteIcon=📥`

#### POST `/api/favorites/{messageId}`
- 需要认证
- 当前将指定消息加入收藏；若已收藏则返回现有记录

#### DELETE `/api/favorites/{messageId}`
- 需要认证
- 当前取消指定消息的收藏

### 同步

#### POST `/api/sync/bootstrap`
- 需要认证
- 返回 profile、notes、collections、messages、favorites、serverTime、bootstrap 标记

#### POST `/api/sync/pull`
- 需要认证
- 当前返回 profile、notes、collections、messages、favorites、serverTime

#### POST `/api/sync/push`
- 需要认证
- 当前接收 notes / collections / messages / favorites payload，并返回 processed 计数与 serverTime

## 当前已实现接口
### 认证
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 用户名 + 密码登录 |
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/refresh` | 刷新 token（JSON body: `{ "refreshToken": "..." }`） |
| POST | `/api/auth/logout` | 登出 |

### 用户
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/users/profile` | 获取个人资料 |
| PUT | `/api/users/profile` | 更新个人资料 |
| GET | `/api/users/contacts` | 获取联系人列表 |
| GET | `/api/users/contacts/requests` | 获取待处理好友请求 |
| GET | `/api/users/contacts/requests/count` | 获取待处理好友请求数 |
| POST | `/api/users/contacts/request` | 发起好友请求 |
| POST | `/api/users/contacts/request/accept` | 接受好友请求 |
| POST | `/api/users/contacts/request/reject` | 拒绝好友请求 |
| DELETE | `/api/users/contacts/request/{requestId}` | 取消好友申请 |
| DELETE | `/api/users/contacts/{contactUserId}` | 删除联系人/取消申请 |
| GET | `/api/users/contacts/search` | 搜索用户并返回关系状态 |

### 闪记
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/flash-notes/list` | 获取闪记列表 |
| POST | `/api/flash-notes/search` | 搜索闪记及消息 |
| POST | `/api/flash-notes` | 创建闪记 |
| PUT | `/api/flash-notes/{id}` | 更新闪记 |
| PUT | `/api/flash-notes/{id}/pin` | 置顶/取消置顶 |
| PUT | `/api/flash-notes/{id}/hide` | 隐藏/取消隐藏 |
| DELETE | `/api/flash-notes/{id}` | 删除闪记 |

### 消息
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/messages/list` | 获取消息列表 |
| POST | `/api/messages` | 发送基础消息 |
| GET | `/api/messages/stream` | SSE 消息流 |
| DELETE | `/api/messages/{id}` | 删除消息 |

### 合集
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/collections/list` | 获取合集列表 |
| POST | `/api/collections` | 创建合集 |
| PUT | `/api/collections/{id}` | 更新合集 |
| DELETE | `/api/collections/{id}` | 删除合集 |

### 文件
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/files/upload` | 文件上传 |
| GET | `/api/files/download?objectName=...` | 文件下载 |

### 收藏
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/favorites/list` | 获取收藏消息列表 |
| POST | `/api/favorites/{messageId}` | 收藏消息 |
| DELETE | `/api/favorites/{messageId}` | 取消收藏 |

### 同步
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/sync/bootstrap` | 基础同步初始化 |
| POST | `/api/sync/pull` | 拉取同步数据 |
| POST | `/api/sync/push` | 推送同步数据 |

## 当前未实现但设计存在的接口
- 验证码发送、验证码校验、重置密码
- 收藏相关接口已落地基础消息收藏闭环
- 搜索接口
- 系统健康与系统信息接口（文档级设计）
- 多媒体消息细分接口（text/image/audio/video/file/mixed）

## 历史漂移说明
- 旧文档中的 `/api/v1/...` 全部视为过时路径
- 旧文档中的 email 登录描述视为过时，当前代码使用 username 登录
- Android 侧 API 声明并不自动等于后端真相，联调时始终以 controller 为准

## 维护规则
- 任何新增接口，先更新“当前已实现接口”部分
- 任何尚未实现但已确定保留的能力，放到“当前未实现但设计存在的接口”部分
- 不再把目标态接口写成“当前可用”语气
- 请求体、响应体、错误码、认证要求发生变化时，必须同步更新本文件
