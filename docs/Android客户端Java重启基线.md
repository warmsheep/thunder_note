# Android 客户端 Java 基线

## 文档定位
本文档记录 Android Java 客户端的当前实现状态、边界与经验参考。

## 当前状态结论
- 当前仓库内的 `thunder-note-android/` 是 **Java 客户端主模块**。
- 当前 Java 客户端已具备：Splash / Login / Register / Main / FlashNote / Chat 主壳层，并可完成独立构建。
- 当前 Java 客户端尚未完整补齐：完整 Profile 真实业务内容、Room 主链、本地离线闭环；Collection、Favorite、最小 Sync 已接入真实链路。

## 必须继承的后端事实
- 后端 API 当前真实路径为 `/api/...`，不是 `/api/v1/...`
- 登录语义按 `username`，不是邮箱
- 后端当前真实主线模块：`auth / user / flashnote / message / collection / file / sync`
- `sync` 当前已有最小闭环与手动触发入口，但仍不是完整离线同步体系

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
