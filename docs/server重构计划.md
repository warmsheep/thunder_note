# Thunder Note Server 重构计划

> 生成时间：2026-03-28
> 基于代码审查报告创建

## 目录
1. [安全修复](#1-安全修复高优先级)
2. [代码质量重构](#2-代码质量重构中优先级)
3. [依赖更新](#3-依赖更新低优先级)
4. [执行记录](#4-执行记录)

---

## 1. 安全修复（高优先级）

### 🔴 TODO-1: application.yml 密钥迁移到环境变量 ✅
（见 Round 1）

---

### 🔴 TODO-2: CORS 配置改为白名单模式 ✅
（见 Round 1）

---

### 🟡 TODO-3: UserController.updateAvatar 输入校验 ✅
（见 Round 1）

---

### 🟡 TODO-4: SyncController/SyncServiceImpl 强类型 DTO ✅
（见 Round 1）

---

### 🟡 TODO-5: Actuator 端点暴露限制 ✅
（见 Round 1）

---

### 🔴 TODO-R3-1: 扩展限流覆盖 register 和 refresh 接口 ✅ **Round 3 新增**
**风险等级**：高
**问题**：`RateLimitInterceptor` 仅覆盖 `/api/auth/login`，注册和刷新令牌接口无保护
**操作**：
- [x] 将限流路径扩展至 Set.of("/api/auth/login", "/api/auth/register", "/api/auth/refresh")
- [x] Redis key 从 `ratelimit:login:ip` 改为 `ratelimit:auth:ip`
- [x] WebConfig 注册所有 3 个路径

**涉及文件**：
- `common/ratelimit/RateLimitInterceptor.java`
- `common/config/WebConfig.java`

---

### 🔴 TODO-R3-2: 文件上传大小和类型校验 ✅ **Round 3 新增**
**风险等级**：高
**问题**：`FileServiceImpl.upload()` 无文件大小和类型限制，可上传任意大小和类型的文件
**操作**：
- [x] 新增 MAX_FILE_SIZE = 10MB 常量
- [x] 新增 ALLOWED_TYPES 白名单：image/jpeg, image/png, image/gif, image/webp, video/mp4, audio/mpeg, audio/mp4, application/pdf
- [x] 上传前校验，超限抛 BAD_REQUEST

**涉及文件**：
- `file/service/impl/FileServiceImpl.java`

---

### 🔴 TODO-R3-3: 文件下载路径遍历修复 ✅ **Round 3 新增**
**风险等级**：高
**问题**：`normalizeObjectName()` 不拒绝 `../` 序列，可构造路径遍历请求
**操作**：
- [x] 新增 `containsTraversal()` 方法，URL 解码后拒绝含 `..` 的路径
- [x] 下载和删除操作均经过校验，非法路径抛 BAD_REQUEST

**涉及文件**：
- `file/service/impl/FileServiceImpl.java`

---

## 2. 代码质量重构（中优先级）

### 🟢 TODO-6: Lombok 引入并重构实体类 ✅
（见 Round 1）

---

### 🟢 TODO-7: MessageServiceImpl 分页改为 IPage ✅
（见 Round 1）

---

### 🟢 TODO-8: 异常吞掉问题修复 ✅
（见 Round 1）

---

### 🟢 TODO-R3-4: CollectionServiceImpl N+1 查询优化 ✅ **Round 3 新增**
**风险等级**：高
**问题**：`cascadeRename` 和 `cascadeClear` 先 SELECT 全量数据，再循环逐条 UPDATE（经典 N+1）
**操作**：
- [x] `cascadeRename` → `flashNoteMapper.update(null, LambdaUpdateWrapper)` 单条 SQL 批量更新
- [x] `cascadeClear` → 同上，将 tags 置为 null

**涉及文件**：
- `collection/service/impl/CollectionServiceImpl.java`

---

### 🟢 TODO-R3-5: .last("LIMIT") 反模式修复 ✅ **Round 3 新增**
**风险等级**：中
**问题**：多处使用 `.last("LIMIT X")` 绕过 MyBatis-Plus 分页 API
**操作**：
- [x] `FlashNoteServiceImpl.getMessageContext`：`.last("LIMIT 3")` → `selectPage(Page<>(1,3))`
- [x] `FlashNoteServiceImpl.latestInboxMessage`：`.last("LIMIT 1")` → `selectOne`
- [x] `UserServiceImpl.findLatestConversationMessage`：`.last("LIMIT 1")` → `selectOne`
- [x] `UserServiceImpl.searchUsers`：`.last("LIMIT 30")` → `selectPage(Page<>(1,30))`
- [x] `UserServiceImpl.findPair`：移除冗余 `.last("LIMIT 1")`（selectOne 内部保证）

**涉及文件**：
- `flashnote/service/impl/FlashNoteServiceImpl.java`
- `user/service/impl/UserServiceImpl.java`

---

### 🟢 TODO-R3-6: COLLECTION_BOX_NOTE_ID 常量集中化 ✅ **Round 3 新增**
**风险等级**：中
**问题**：`COLLECTION_BOX_NOTE_ID = -1L` 在 3 个 service 中各有定义
**操作**：
- [x] 新建 `common/constant/NoteConstants`，包含 `COLLECTION_BOX_NOTE_ID`、`COLLECTION_BOX_TITLE`、`COLLECTION_BOX_ICON`
- [x] `FlashNoteServiceImpl`、`MessageServiceImpl`、`FavoriteServiceImpl` 全部引用 `NoteConstants`

**涉及文件**：
- 新建 `common/constant/NoteConstants.java`
- `flashnote/service/impl/FlashNoteServiceImpl.java`
- `message/service/impl/MessageServiceImpl.java`
- `favorite/service/impl/FavoriteServiceImpl.java`

---

### 🟢 TODO-R3-7: CurrentUserService 统一用户查询 ✅ **Round 3 新增**
**风险等级**：中
**问题**：`getRequiredUserId()` 和 `getRequiredUser()` 在 5 个 service 中完全相同
**操作**：
- [x] 新建 `common/service/CurrentUserService`，提供 `getRequiredUserId(username)` 和 `getRequiredUser(username)` 方法
- [x] `FlashNoteServiceImpl`、`MessageServiceImpl`、`FavoriteServiceImpl`、`CollectionServiceImpl`、`FileServiceImpl`、`UserServiceImpl` 全部注入并使用

**涉及文件**：
- 新建 `common/service/CurrentUserService.java`
- `flashnote/service/impl/FlashNoteServiceImpl.java`
- `message/service/impl/MessageServiceImpl.java`
- `favorite/service/impl/FavoriteServiceImpl.java`
- `collection/service/impl/CollectionServiceImpl.java`
- `file/service/impl/FileServiceImpl.java`
- `user/service/impl/UserServiceImpl.java`

---

### 🟢 TODO-R3-8: MediaType 枚举统一媒体类型显示 ✅ **Round 3 新增**
**风险等级**：低
**问题**：`[图片]`、`[视频]`、`[语音]` 等显示文本逻辑在 3 个 service 中重复
**操作**：
- [x] 新建 `common/constant/MediaType` 枚举，包含 TEXT/IMAGE/VIDEO/VOICE/FILE/COMPOSITE 及 displayText
- [x] `FlashNoteServiceImpl.resolveLatestMessage()` → `MediaType.resolveDisplay()`
- [x] `UserServiceImpl.resolveLatestMessage()` → `MediaType.resolveDisplay()`
- [x] `MessageServiceImpl.sendMessage()` switch → `MediaType.resolveDisplay()`

**涉及文件**：
- 新建 `common/constant/MediaType.java`
- `flashnote/service/impl/FlashNoteServiceImpl.java`
- `user/service/impl/UserServiceImpl.java`
- `message/service/impl/MessageServiceImpl.java`

---

## 3. 依赖更新（低优先级）

### 🔵 TODO-9: 依赖版本检查与更新 ✅
（见 Round 1）

---

### 🔵 TODO-R3-9: dependency-check-maven 插件 ✅ **Round 3 新增**
**操作**：
- [x] 在 `pom.xml` 新增 `org.owasp:dependency-check-maven:9.2.0`
- [x] `failBuildOnAnyVulnerability=false`（报告模式）

**涉及文件**：
- `pom.xml`

---

## 4. Round 4 执行项

### 🟢 TODO-R4-1: Controller 响应 DTO 化 ✅ **Round 4 新增**
**风险等级**：中
**问题**：`FlashNoteController`、`MessageController`、`CollectionController`、`UserController` 仍直接返回实体对象，API 契约与数据库实体耦合。
**操作**：
- [x] 新增 `FlashNoteResponse`、`FlashNoteSearchResponseData`、`FlashNoteSearchResultResponse`、`MatchedMessageInfoResponse`
- [x] 新增 `MessageResponse`
- [x] 新增 `CollectionResponse`
- [x] 新增 `UserProfileResponse`
- [x] 在 4 个 controller 内完成 entity → DTO 映射，避免改动 service 接口签名，降低回归面

**涉及文件**：
- 新建 `flashnote/dto/FlashNoteResponse.java`
- 新建 `flashnote/dto/FlashNoteSearchResponseData.java`
- 新建 `flashnote/dto/FlashNoteSearchResultResponse.java`
- 新建 `flashnote/dto/MatchedMessageInfoResponse.java`
- 新建 `message/dto/MessageResponse.java`
- 新建 `collection/dto/CollectionResponse.java`
- 新建 `user/dto/UserProfileResponse.java`
- `flashnote/controller/FlashNoteController.java`
- `message/controller/MessageController.java`
- `collection/controller/CollectionController.java`
- `user/controller/UserController.java`

---

### 🟢 TODO-R4-2: FileService 单元测试 ✅ **Round 4 新增**
**风险等级**：低
**问题**：Round 3 新增的上传校验与路径安全逻辑没有测试保护。
**操作**：
- [x] 新增 `FileServiceImplTest`
- [x] 覆盖上传成功、超限文件、非法类型、下载失败映射、空对象删除、合法删除、路径遍历拒绝等场景

**涉及文件**：
- 新建 `src/test/java/com/flashnote/file/FileServiceImplTest.java`

---

### 🟢 TODO-R4-3: UserService 单元测试 ✅ **Round 4 新增**
**风险等级**：低
**问题**：`UserServiceImpl` 之前没有任何测试，联系人、资料、搜索等逻辑缺少回归保护。
**操作**：
- [x] 新增 `UserServiceImplTest`
- [x] 覆盖 `getProfile`、`updateProfile`、`searchUsers`、`listContacts`、`sendFriendRequest`、`countPendingRequests`、`cancelFriendRequest` 等关键路径

**涉及文件**：
- 新建 `src/test/java/com/flashnote/user/UserServiceImplTest.java`

---

### 🟢 TODO-R4-4: MediaType 扩展 gif/webp ✅ **Round 4 新增**
**风险等级**：低
**问题**：文件白名单已允许 `image/gif` 和 `image/webp`，但 `MediaType` 枚举未覆盖对应展示语义。
**操作**：
- [x] 在 `MediaType` 中新增 `GIF` 和 `WEBP`
- [x] 两者统一展示为 `[图片]`

**涉及文件**：
- `common/constant/MediaType.java`

---

## 5. 执行记录

| TODO | 状态 | 执行人 | 完成日期 | 备注 |
|------|------|--------|----------|------|
| TODO-1 | ✅ 已完成 | Sisyphus | 2026-03-28 | 密钥迁移至环境变量 |
| TODO-2 | ✅ 已完成 | Sisyphus | 2026-03-28 | CORS 白名单模式 |
| TODO-3 | ✅ 已完成 | Sisyphus | 2026-03-28 | Avatar 输入校验 DTO |
| TODO-4 | ✅ 已完成 | Sisyphus | 2026-03-28 | 强类型 DTO 改造 |
| TODO-5 | ✅ 已完成 | Sisyphus | 2026-03-28 | Actuator 端点暴露限制 |
| TODO-6 | ✅ 已完成 | Sisyphus | 2026-03-28 | Lombok @Getter/@Setter/@Data 引入并重构所有实体 |
| TODO-7 | ✅ 已完成 | Sisyphus | 2026-03-28 | MessageServiceImpl 分页改为 MyBatis-Plus IPage |
| TODO-8 | ✅ 已完成 | Sisyphus | 2026-03-28 | 异常日志修复（DuplicateKeyException + MIME 解析） |
| TODO-9 | ✅ 已完成 | Sisyphus | 2026-03-28 | Spring Boot 3.2.6→3.2.12, MinIO 8.5.11→8.5.17 |
| TODO-R3-1 | ✅ 已完成 | Sisyphus | 2026-03-29 | 扩展限流覆盖注册和刷新接口 |
| TODO-R3-2 | ✅ 已完成 | Sisyphus | 2026-03-29 | 文件上传大小和类型白名单校验 |
| TODO-R3-3 | ✅ 已完成 | Sisyphus | 2026-03-29 | 修复文件下载路径遍历漏洞 |
| TODO-R3-4 | ✅ 已完成 | Sisyphus | 2026-03-29 | CollectionServiceImpl N+1 查询优化（批量 UPDATE） |
| TODO-R3-5 | ✅ 已完成 | Sisyphus | 2026-03-29 | 替换 .last(LIMIT) 反模式为标准分页 API |
| TODO-R3-6 | ✅ 已完成 | Sisyphus | 2026-03-29 | 集中 COLLECTION_BOX_NOTE_ID 常量到 NoteConstants |
| TODO-R3-7 | ✅ 已完成 | Sisyphus | 2026-03-29 | 统一用户查询到 CurrentUserService |
| TODO-R3-8 | ✅ 已完成 | Sisyphus | 2026-03-29 | 统一媒体类型显示到 MediaType 枚举 |
| TODO-R3-9 | ✅ 已完成 | Sisyphus | 2026-03-29 | 新增 dependency-check-maven 漏洞扫描插件 |
| TODO-R4-1 | ✅ 已完成 | Sisyphus | 2026-03-29 | 4 个 controller 响应 DTO 化，映射下沉到 controller 层 |
| TODO-R4-2 | ✅ 已完成 | Sisyphus | 2026-03-29 | 新增 FileServiceImplTest，覆盖上传与路径安全逻辑 |
| TODO-R4-3 | ✅ 已完成 | Sisyphus | 2026-03-29 | 新增 UserServiceImplTest，覆盖资料/搜索/联系人关键路径 |
| TODO-R4-4 | ✅ 已完成 | Sisyphus | 2026-03-29 | MediaType 新增 GIF/WEBP 并统一图片展示 |
| TODO-R4-5 | ✅ 已完成 | Sisyphus | 2026-03-29 | Android 侧已对齐 Round 4 DTO 契约：消息分页、FlashNote.deleted、UserProfile LocalDateTime |
| TODO-R4-6 | ✅ 已完成 | Sisyphus | 2026-03-29 | Android 调用层 API 契约全量对齐：Auth/User/Sync 弱类型 body 收敛为 DTO，消息计数改 Long |

---

## 6. 遗留项（Round 5 及以后）

| 任务 | 优先级 | 说明 |
|------|--------|------|
| Search 响应深度 DTO 化 | Medium | `FlashNoteSearchResponse` 当前 controller 已映射为 DTO，但 service 内部搜索模型仍保留 entity 作为中间态 |
| Message payload 精细 DTO | Low | 当前 `MessageResponse` 仍直接承载 `CardPayload`，若后续要彻底脱离实体实现可继续拆分 |
| 消息 SSE 实时订阅接入 | Low | 后端已提供 `GET /api/messages/stream`，Android 当前主链尚未消费；属于能力缺口，不是现有接口漂移 |

---

*最后更新：2026-03-29 Round 4 + Android 全量接口契约对齐* 
