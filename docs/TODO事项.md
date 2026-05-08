# 闪记路线图

## 文档定位
本文档是**路线图摘要**，不是当前实现清单，也不是执行型 backlog。只有标为"当前已完成"的事项，才代表仓库里已有明确落地；详细开发优先级与执行任务以 `完整开发计划.md` 为准。

## 当前已完成基础
- Spring Boot 后端项目初始化
- PostgreSQL / Redis / MinIO / Flyway 基础接入
- auth / user / flashnote / message / collection / file / sync / favorite 基础模块已实现
- Android Java 客户端主模块初始化并接管 `thunder-note-android/`
- Java 版认证主链、闪记列表、聊天、合集、收藏与 sync/file 入口已落地
- Java 版用户资料（profile）已接入真实后端
- 后端 auth / flashnote 集成测试已补齐
- Android auth / flashnote repository 测试已补齐
- HTML 原型迭代

## UI 样式优化待办
- [ ] 底部导航图标选中时不变色（已尝试 colorControlNormal 未生效，需进一步排查）

## MVP 阶段（已完成 ✅）

### A0-A7 全部完成
- Android Java 客户端基线已建立
- 接口事实与客户端声明已统一
- 认证闭环已完成
- 闪记闭环已完成
- 消息闭环已完成
- 合集闭环已完成
- 文件基础闭环已完成
- 最小同步闭环已完成
- 用户资料接口已接入

## 增强版阶段（进行中）

### B1-B5 待完成
- 多媒体消息基础版
- 同步可靠性增强
- 用户体验补齐
- 测试与质量基础建设

## 中期规划
- 完整同步协议落地
- 搜索能力
- 收藏能力增强：跨类型收藏、与同步联动
- 更完整的多媒体消息能力

## 远期规划
- Web 用户端：已明确后续写入 `thunder_note_server/`，同一 Spring Boot 服务同时提供 Android API 与 Web 页面；详细执行计划见 `完整开发计划.md` 的 `D1`
- iOS 客户端
- 管理后台

## 维护规则
- 路线图只写阶段与方向，不再维护巨量逐条接口勾选表
- 当前实现事实请查看代码、`AGENTS.md` 与当前态设计文档
- 当某项真正落地后，应同步移动到"当前已完成基础"或相应设计文档中
- MVP 完成检查清单已于 2026-03-15 完成
