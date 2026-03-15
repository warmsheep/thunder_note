# 闪记路线图

## 文档定位
本文档是**路线图摘要**，不是当前实现清单，也不是执行型 backlog。只有标为“当前已完成”的事项，才代表仓库里已有明确落地；详细开发优先级与执行任务以 `完整开发计划.md` 为准。

## 当前已完成基础
- Spring Boot 后端项目初始化
- PostgreSQL / Redis / MinIO / Flyway 基础接入
- auth / user / flashnote / message / collection / file / sync 基础模块骨架
- Android Java 客户端主模块初始化并接管 `thunder-note-android/`
- Java 版认证主链、主壳层、闪记列表、聊天、合集与最小 sync/file 入口已落地
- HTML 原型迭代

## 当前优先事项
### P0：补齐 Java 客户端主链
- 将 FlashNote / Chat 从内存数据切到真实后端链路
- 继续完善完整 Profile 体验；Favorite 与 Collection 已接通，Profile 仅保留 MVP 操作面
- 明确当前 Java 客户端与后端的新联调执行基线

### P1：完善 Java 客户端工程能力
- 补齐 Room / 本地缓存是否回归主线的决策
- 建立 Java 客户端测试、Lint、构建与发布检查清单
- 建立 Java 客户端测试、Lint、构建与发布检查清单

### P2：后端稳定与跨端扩展准备
- 闪记/消息/合集/文件/同步的后端事实继续维护
- 为 Java 客户端后续模块补齐准备更清晰的接口与 DTO 基线
- 为 Web/iOS/admin 的远期阶段保留稳定数据契约

## 中期规划
- 完整同步协议落地
- 搜索能力
- 收藏能力增强：跨类型收藏、与同步联动
- 更完整的多媒体消息能力

## 远期规划
- Web 用户端
- iOS 客户端
- 管理后台

## 维护规则
- 路线图只写阶段与方向，不再维护巨量逐条接口勾选表
- 当前实现事实请查看代码、`AGENTS.md` 与当前态设计文档
- 当某项真正落地后，应同步移动到“当前已完成基础”或相应设计文档中
