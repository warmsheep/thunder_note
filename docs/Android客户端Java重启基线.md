# Android 客户端 Java 基线

## 文档定位
本文档记录 Android Java 客户端的当前实现状态、边界与经验参考。

## 当前状态结论
- 当前仓库内的 `thunder-note-android/` 是 **Java 客户端主模块**。
- 当前 Java 客户端已具备：Splash / Login / Register / Main / FlashNote / Chat 主壳层，并可完成独立构建。
- 当前 Java 客户端尚未完整补齐：完整 Profile 真实业务内容、Room 主链、本地离线闭环；Collection、Favorite、最小 Sync 已接入真实链路。

## 当前架构风险提醒（2026-03-23 复核）
- 当前 `chat/message` 与 `flashnote list` 链路已经暴露出**数据流层架构缺陷**，不是单点 UI 小 bug：
  - 聊天消息与上传状态仍大量依赖内存 `LiveData + HashMap`；
  - 会话重绑时存在“先清空列表再拉远端”的实现；
  - 闪记列表刷新仍与 Fragment / ViewModel 生命周期强绑定；
  - 当前 sync/file 仅是 MVP 最小闭环，不能误判为完整离线优先架构。
- 因此，后续新会话如果碰到以下问题：
  - 上传中状态丢失
  - 聊天页空白
  - 闪记列表闲置重复请求
  不应只做零散补丁，必须先查看 `docs/完整开发计划.md` 中 **3.5 Android 聊天 / 上传 / 闪记列表数据流重构专项**。

## 当前模块成熟度快照（2026-03-16 代码核对）
| 模块 | 成熟度 | 代码依据 | 备注 |
|------|--------|----------|------|
| auth | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/AuthRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/auth/` | 已接登录、注册、刷新、登出、Token 持久化与启动态判断 |
| flashnote | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/FlashNoteRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/main/FlashNoteTabFragment.java` | 已接列表、创建、编辑、删除、进入聊天 |
| chat/message | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/MessageRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/chat/` | 已按 `flashNoteId` 拉取并发送基础文本消息 |
| collection | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/CollectionRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/main/CollectionTabFragment.java` | 已接合集列表、创建、编辑、删除 |
| favorite | 真实主链 | `app/src/main/java/com/flashnote/java/data/repository/FavoriteRepositoryImpl.java`、`app/src/main/java/com/flashnote/java/ui/main/FavoriteTabFragment.java` | 已接消息收藏列表、移除、从收藏跳转回聊天 |
| profile | 部分实现 | `app/src/main/java/com/flashnote/java/ui/main/ProfileTabFragment.java`、`app/src/main/java/com/flashnote/java/data/repository/UserRepositoryImpl.java` | 已接资料查询/更新、手动 sync/file/logout 入口，但仍是操作台式页面 |
| sync | MVP 最小闭环 | `app/src/main/java/com/flashnote/java/data/repository/SyncRepositoryImpl.java` | 已接 `bootstrap / pull / push`，仍未进入完整离线队列体系 |
| file | MVP 最小闭环 | `app/src/main/java/com/flashnote/java/data/repository/FileRepositoryImpl.java` | 已接上传并回读下载 |

## 当前页面与导航范围快照（2026-03-16 代码核对）
- 应用入口：`MainActivity` 首次进入打开 `SplashFragment`。
- 启动判断：`SplashFragment` 通过 `TokenManager.isTokenValid()` 决定进入 `LoginFragment` 或 `MainShellFragment`。
- 认证流：`LoginFragment` 可跳转 `RegisterFragment`；`RegisterFragment` 注册成功后自动登录并进入主壳。
- 主壳流：`MainShellFragment` 维护四个底部 Tab：`FlashNoteTabFragment`、`CollectionTabFragment`、`FavoriteTabFragment`、`ProfileTabFragment`。
- 内容流：`FlashNoteTabFragment` 支持新建/编辑/删除闪记，并通过 `ShellNavigator.openChat()` 进入 `ChatFragment`。
- 收藏流：`FavoriteTabFragment` 可基于已收藏消息重新打开关联闪记聊天。
- 个人中心流：`ProfileTabFragment` 支持刷新资料、编辑 bio、手动触发 `bootstrap/pull/push`、文件上传回读、退出登录。

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

## 当前本地临时语义与不应误判为主线字段
- Android 当前主链没有把 Room、离线待同步队列、`syncStatus / localUpdatedAt / lastSyncVersion` 拉回主线。
- MVP 阶段仍不支持 `pinned / hidden`，不要把旧模型里的扩展字段当成当前必备能力。
- Profile 当前只暴露 bio 与操作入口，未形成完整个人中心信息编辑体系。
- Sync `push` 当前上传的是内存中的 `notes / collections / messages / favorites` 快照，不等于完整操作日志同步协议。
- 当前聊天上传中的“本地立即可见”能力还不是完整本地持久化 outbox，只是过渡态；在专项重构完成前，不要把它当成稳定的离线优先能力。

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

## Java 客户端默认原则
- 先以后端当前真实接口和文档为准建立 DTO 和 repository
- 先重建最小可用链路，再谈离线缓存，同步和增强体验
- 先定义页面范围与导航，再确定 UI 技术方案细节
- 先区分真实链路与占位能力，避免再次出现"页面在但业务未通"的误判
- 如果进入聊天 / 上传 / 闪记列表专项治理，优先执行 `完整开发计划.md` 的 P0 → P1 → P2 顺序，禁止跳阶段直接叠加复杂 sync 逻辑

## 推荐的 Java 客户端技术基线
- Android 语言：Java
- UI：XML + ViewBinding
- 页面组织：Activity / Fragment + Navigation
- UI 状态：ViewModel + LiveData
- 网络：Retrofit + OkHttp
- 本地存储：Room（仅在最小链路稳定后再接入）
- Token 持久化：加密存储
- DI：优先选"团队易理解、易调试"的方案

## 当前不提前锁死的技术决策
- 是否使用 Hilt，还是手动 DI
- 是否在第一阶段就引入 Room，还是先跑纯远程闭环
- 是否在第一阶段就接入同步/离线队列

## 推荐的 Java 起步顺序
1. 建立 Android Java 工程基础骨架
2. 建立认证最小闭环：登录、注册、登出、token 持久化
3. 建立闪记最小闭环：列表、创建、更新、删除
4. 建立基础消息链路
5. 建立合集基础能力
6. 最后再决定文件、同步、本地缓存等增强能力如何接入

## 启动前必读文档
- `docs/完整开发计划.md`
- `docs/需求分析.md`
- `docs/架构设计.md`
- `docs/客户端架构设计.md`
- `docs/开发测试部署经验库.md`
