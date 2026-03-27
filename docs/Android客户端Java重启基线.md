# Android 客户端 Java 基线

## 文档定位
本文档记录 Android Java 客户端的当前实现状态、边界与经验参考。

## 当前状态结论
- 当前仓库内的 `thunder-note-android/` 是 **Java 客户端主模块**。
- 当前 Java 客户端已具备：Splash / Login / Register / Main / FlashNote / Chat 主壳层，并可完成独立构建。
- 当前 Java 客户端的 `auth / flashnote / chat / collection / favorite / profile` 已不再是简单壳层：主链 repository、页面和本地表/同步入口均已接通当前真实实现。
- 当前尚未彻底完成的部分，重点不在“页面壳层”，而在：
  - 数据流与离线架构的继续收口（尤其 sync、恢复链、生命周期边界）；
  - `profile` 从操作台式页面继续收敛为更清晰的个人中心结构；
  - 工程治理项，如错误处理、资源化、重复模板抽取、测试补强。
- 后续 Android 开发默认先看 `docs/完整开发计划.md` 的 **3.5.8 Android 剩余可执行 backlog**，再决定本次会话认领哪一条任务。

## 当前风险提醒
- Android 当前已不是“聊天空白 / 假数据壳层”阶段，P0/P1/P2 核心骨架已经落地。
- 当前剩余风险主要在：
  - sync payload 与后台恢复链继续收口；
  - `state-saved` / 异步 UI 回调边界继续统一；
  - `profile` 从操作台式页面继续收敛；
  - 恢复链真机补证与测试矩阵继续补强。
- 后续 Android 会话默认先看：
  - `docs/完整开发计划.md` 的 **3.5.7 Android 代码审查整改专项**
  - `docs/完整开发计划.md` 的 **3.5.8 Android 剩余可执行 backlog**

## 当前模块成熟度快照（2026-03-16 代码核对）
| 模块 | 成熟度 | 代码依据 | 备注 |
|------|--------|----------|------|
| auth | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/AuthRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/auth/` | 已接登录、注册、刷新、登出、Token 持久化与启动态判断 |
| flashnote | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/FlashNoteRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/main/FlashNoteTabFragment.java` | 已接列表、创建、编辑、删除、进入聊天 |
| chat/message | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/MessageRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/chat/` | 已按 `flashNoteId` 拉取并发送基础文本消息 |
| collection | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/CollectionRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/main/CollectionTabFragment.java` | 已接合集列表、创建、编辑、删除 |
| favorite | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/FavoriteRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/main/FavoriteTabFragment.java` | 已接消息收藏列表、移除、从收藏跳转回聊天 |
| profile | 部分实现 | `app/src/main/java/com/flashnote/java/ui/main/ProfileTabFragment.java`、`app/src/main/java/com/flashnote/java/data/repository/UserRepositoryImpl.java` | 已接资料查询/更新、头像区右上角同步入口、设置页日志查看、退出登录，但仍是操作台式页面 |
| sync | MVP 最小闭环 | `app/src/main/java/com/flashnote/java/data/repository/SyncRepositoryImpl.java` | 已接 `pullAndRefreshLocal / syncAll / 本地真源 push`，支持待同步数量展示，仍未进入完整离线冲突解决体系 |
| file | 真实基础能力（非独立调试页） | `app/src/main/java/com/flashnote/java/data/repository/FileRepositoryImpl.java` | 上传/下载能力仍服务于头像、聊天媒体等主链，但已不再保留单独文件调试页面 |

## 当前页面与导航范围快照（2026-03-16 代码核对）
- 应用入口：`MainActivity` 首次进入打开 `SplashFragment`。
- 启动判断：`SplashFragment` 通过 `TokenManager.isTokenValid()` 决定进入 `LoginFragment` 或 `MainShellFragment`。
- 认证流：`LoginFragment` 可跳转 `RegisterFragment`；`RegisterFragment` 注册成功后自动登录并进入主壳。
- 主壳流：`MainShellFragment` 维护四个底部 Tab：`FlashNoteTabFragment`、`CollectionTabFragment`、`FavoriteTabFragment`、`ProfileTabFragment`。
- 内容流：`FlashNoteTabFragment` 支持新建/编辑/删除闪记，并通过 `ShellNavigator.openChat()` 进入 `ChatFragment`。
- 收藏流：`FavoriteTabFragment` 可基于已收藏消息重新打开关联闪记聊天。
- 个人中心流：`ProfileTabFragment` 支持刷新资料、编辑个人资料、头部右上角一键同步、退出登录。
- 设置流：`SettingsFragment` 当前可进入独立日志查看页，不再从“我的”页保留调试工具入口。

## 必须继承的后端事实
- 后端 API 当前真实路径为 `/api/...`，不是 `/api/v1/...`
- 登录语义按 `username`，不是邮箱
- 后端当前真实主线模块：`auth / user / flashnote / message / collection / file / sync`
- `sync` 当前已有最小闭环与手动触发入口，但仍不是完整离线同步体系

## Java 客户端必须继承的当前契约事实
- `AuthController` 当前真实路径为 `/api/auth/login`、`/api/auth/register`、`/api/auth/refresh`、`/api/auth/logout`；Android 对应声明位于 `app/src/main/java/com/flashnote/java/data/remote/AuthService.java`。
- `UserController` 当前资料接口为 `POST /api/users/profile` 与 `PUT /api/users/profile`；Android 对应声明位于 `app/src/main/java/com/flashnote/java/data/remote/UserService.java`。
- `FlashNoteController` 当前真实路径前缀为 `/api/flash-notes`；Android 对应声明位于 `app/src/main/java/com/flashnote/java/data/remote/FlashNoteService.java`。
- `MessageController` 当前 MVP 主链使用 `POST /api/messages/list` 与 `POST /api/messages`；`GET /api/messages/stream` 已存在于后端，但 Android 当前 MVP 未接入 SSE。
- `CollectionController`、`FileController`、`SyncController`、`FavoriteController` 当前真实路径均已在 Android `data/remote/` 中接通。

## 当前边界与仍未完成项
- Room 已进入 `flashnote / message / favorite / collection / pending` 主链，但这不等于完整离线协议已经完成。
- `syncAll()` 已改为从本地真源表收集 payload；剩余问题在 payload 继续收口、Worker 恢复补证与失败语义统一。
- `PendingRecoveryWorker` 与 `pullAndRefreshLocal()` 已形成最小后台恢复链；剩余问题在跨重启真机补证与更完整的恢复/推送语义。
- `profile` 已具备真实资料、同步、头像、设置入口，但仍偏“操作台式页面”，后续继续收敛结构即可。
- MVP 阶段不应把旧模型里的所有扩展字段都当成当前必备能力；当前事实以真实 repository / Room / worker 主链为准。

## 必须保留的产品与页面范围
- 登录 / 注册 / Splash / 主界面
- 闪记列表与基础 CRUD
- 基础消息列表与发送/拉取
- 合集基础管理
- 个人中心 / 设置 等页面范围
- HTML 原型仍是交互和页面范围的重要参考来源

## 当前客户端经验
- `AuthRepositoryImpl` 已有真实网络调用
- `SyncRepositoryImpl` 已接真实接口，但当前仍以手动触发和最小 payload 为主
- 历史上 `CollectionViewModel`、`FavoriteViewModel` 曾存在假数据依赖；当前主线已切到真实后端仓储
- 页面存在不等于业务接通，判断成熟度要看 repository / API / DTO / 真实返回值
- 当前剩余重构的重心已从“壳层接通”转向“数据流收口 + 生命周期稳定性 + 工程治理”，不要再默认把 `collection / favorite / profile` 误判成未接通页面。

## Java 客户端默认原则
- 先以后端当前真实接口和文档为准建立 DTO 和 repository
- 先重建最小可用链路，再谈离线缓存，同步和增强体验
- 先定义页面范围与导航，再确定 UI 技术方案细节
- 先区分真实链路与占位能力，避免再次出现"页面在但业务未通"的误判
- 如果进入聊天 / 上传 / 闪记列表专项治理，优先执行 `完整开发计划.md` 的 P0 → P1 → P2 顺序，禁止跳阶段直接叠加复杂 sync 逻辑

## 当前技术基线
- Android 语言：Java
- UI：XML + ViewBinding
- 页面组织：Activity / Fragment + Navigation
- UI 状态：ViewModel + LiveData
- 网络：Retrofit + OkHttp
- 本地存储：Room（当前主链已接入）
- Token 持久化：加密存储
- DI：优先选"团队易理解、易调试"的方案

## 启动前必读文档
- `docs/完整开发计划.md`
- `docs/需求分析.md`
- `docs/架构设计.md`
- `docs/客户端架构设计.md`
- `docs/开发测试部署经验库.md`
