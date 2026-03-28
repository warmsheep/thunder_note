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

### 🔴 TODO-1: application.yml 密钥迁移到环境变量
**风险等级**：高
**问题**：`application.yml` 中明文硬编码数据库密码、JWT 密钥、MinIO 凭证
**操作**：
- [x] 将 `spring.datasource.password` 改为 `${DB_PASSWORD:postgres}`
- [x] 将 `security.jwt.secret` 改为 `${JWT_SECRET}`（保留本地开发默认值，生产需设置 env var）
- [x] 将 `minio.accessKey` 改为 `${MINIO_ACCESS_KEY:minioadmin}`
- [x] 将 `minio.secretKey` 改为 `${MINIO_SECRET_KEY:minioadmin}`
- [x] 创建 `.env.example` 文件作为模板
- [x] 在 `application.yml` 中添加注释说明必需的环境变量
- [x] 更新 `.env.example` 补充数据库连接配置说明

**⚠️ 注意事项**：环境变量会覆盖 `application.yml` 默认值。**所有环境变量统一使用 `NOTE_` 前缀**避免与其他项目冲突。启动前确认与实际运行环境匹配。本地 docker 容器启动参考：
```bash
NOTE_DB_HOST=172.17.0.1 NOTE_DB_PORT=15432 NOTE_DB_PASSWORD=postgres mvn spring-boot:run
```

**涉及文件**：
- `src/main/resources/application.yml`
- 新建 `.env.example`

---

### 🔴 TODO-2: CORS 配置改为白名单模式
**风险等级**：高
**问题**：`allowedOriginPatterns(List.of("*"))` + `allowCredentials(true)` 组合在生产环境存在风险
**操作**：
- [ ] 将 `allowedOriginPatterns` 改为从环境变量读取：`${CORS_ORIGINS:http://localhost:*}`
- [ ] 生产环境配置实际域名列表（支持多域名逗号分隔）
- [ ] 添加注释说明本地开发与生产配置方式

**涉及文件**：
- `src/main/java/com/flashnote/common/config/CorsConfig.java`
- `src/main/resources/application.yml`

---

### 🟡 TODO-3: UserController.updateAvatar 输入校验
**风险等级**：中
**问题**：`@RequestBody Map<String, String>` 无任何校验，存在 SSRF 风险
**操作**：
- [ ] 创建 `AvatarUpdateRequest` DTO，添加 `@URL` 和 `@Size(max=2048)` 校验
- [ ] 替换 `Map<String, String>` 为 `AvatarUpdateRequest`
- [ ] 添加头像 URL 域名白名单校验（可选，先做基础校验）

**涉及文件**：
- 新建 `user/dto/AvatarUpdateRequest.java`
- `user/controller/UserController.java`

---

### 🟡 TODO-4: SyncController/SyncServiceImpl 强类型 DTO
**风险等级**：中
**问题**：`Map<String, Object>` 牺牲类型安全，难以维护
**操作**：
- [ ] 创建 `SyncPushRequest` 包含 `List<NotePushDto>`、`List<CollectionPushDto>`、`List<MessagePushDto>`、`List<FavoritePushDto>`
- [ ] 创建 `NotePushDto`、`CollectionPushDto`、`MessagePushDto`、`FavoritePushDto` 内部类或独立文件
- [ ] 将 `SyncServiceImpl.push()` 参数从 `Map<String, Object>` 改为 `SyncPushRequest`
- [ ] 将 `SyncController` 的 `Map<String, Object>` 改为 `SyncPushRequest` 并加 `@Valid`
- [ ] 移除 `SyncServiceImpl` 中手动的 `instanceof Number` 类型检查，改用 Jackson 绑定
- [ ] 删除 `processFavorites` 中的 `DuplicateKeyException ignored` 无日志问题

**涉及文件**：
- 新建 `sync/dto/SyncPushRequest.java`
- `sync/controller/SyncController.java`
- `sync/service/SyncService.java`
- `sync/service/impl/SyncServiceImpl.java`

---

### 🟡 TODO-5: Actuator 端点暴露限制
**风险等级**：中
**问题**：`/actuator/env`、`/actuator/beans` 等可能泄露敏感信息
**操作**：
- [ ] 在 `application.yml` 中限制 actuator 暴露端点：`management.endpoints.web.exposure.include=health,info`
- [ ] 对敏感端点添加角色限制（可选）

**涉及文件**：
- `src/main/resources/application.yml`

---

## 2. 代码质量重构（中优先级）

### 🟢 TODO-6: Lombok 引入并重构实体类 ✅
**决定**：采用**方案B**（引入 Lombok 并使用）
**操作**：所有实体类重构为 `@Getter @Setter`（MyBatis-Plus 实体避免 `@Data` 防止 equals/hashCode/toString 触发懒加载），CardItem/CardPayload 用 `@Data`
**重构文件**：User, Collection, FavoriteMessage, FlashNote, UserProfile, FriendRelation, Message, CardItem, CardPayload（共9个）

---

### 🟢 TODO-7: MessageServiceImpl 分页改为 IPage
**问题**：`.last("LIMIT X OFFSET Y")` 绕过 MyBatis-Plus 分页插件，风格不统一
**操作**：
- [ ] 在 `MessageServiceImpl` 中引入 `com.baomidou.mybatisplus.extension.plugins.pagination.Page`
- [ ] 将 `listMessages` 方法改为使用 `Page<Message>` 分页
- [ ] 移除手动的 `offset` 计算和 `.last()` 调用
- [ ] 检查其他 service 是否有类似问题

**涉及文件**：
- `message/service/MessageService.java` — 接口返回类型改为 `IPage<Message>`
- `message/service/impl/MessageServiceImpl.java` — 移除手动 `LIMIT/OFFSET`，改用 `Page<Message>` + `selectPage()`
- `message/controller/MessageController.java` — 响应类型改为 `ApiResponse<IPage<Message>>`

---

### 🟢 TODO-8: 异常吞掉问题修复
**问题**：`catch (DuplicateKeyException ignored)` 和 `catch (Exception ignored)` 无任何日志
**操作**：
- [ ] 在 `FavoriteServiceImpl` 的 `DuplicateKeyException` catch 中添加 `log.debug(...)`
- [ ] 在 `SyncServiceImpl` 的 `DuplicateKeyException` catch 中添加 `log.debug(...)`
- [ ] 在 `FileController` 的 `catch (Exception ignored)` 中添加 `log.warn(...)`（文件操作失败至少要记录）

**涉及文件**：
- `favorite/service/impl/FavoriteServiceImpl.java`
- `sync/service/impl/SyncServiceImpl.java`
- `file/controller/FileController.java`

---

## 3. 依赖更新（低优先级）

### 🔵 TODO-9: 依赖版本检查与更新
**操作**：
- [ ] 运行 `mvn versions:display-dependency-updates` 扫描可更新版本
- [ ] 检查 Spring Boot 3.2.x 最新 patch
- [ ] 检查 MyBatis-Plus 3.5.x 最新版本
- [ ] 检查 MinIO SDK 最新版本（与 server 版本匹配）
- [ ] 可选：引入 `springdoc-openapi-starter-webmvc-ui` 生成 API 文档

**涉及文件**：
- `pom.xml`

---

## 4. 执行记录

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

---

*最后更新：2026-03-28*
