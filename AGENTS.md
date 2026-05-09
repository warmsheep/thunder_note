# 项目知识库

**生成时间：** 2026-03-13 Asia/Shanghai
**仓库状态：** 根目录是 git 仓库，`thunder-note-server/` 与 `thunder-note-android/` 也各自是独立 git 子仓库

## 项目概览
Thunder Note（闪记）当前是"已实现代码 + 目标态规划 + 原型探索"混合仓库。真实代码以 Spring Boot 后端和 Android Java 客户端为主，同时保留大量中文设计文档与一个大型 HTML 原型。

## 目录结构
```text
thunder_note/
├── docs/                    # 产品、架构、API、数据库，同步，设计文档
├── thunder-note-server/     # Spring Boot 后端
├── thunder-note-android/    # Android 客户端
├── thunder-note-prototype.html  # 交互原型
├── scripts/                 # 调试/自动化脚本
└── artifacts/               # 生成物，不是事实来源
```

## 事实来源优先级
发生冲突时，除非任务明确是"只处理文档"，否则按下面顺序判断：

1. 已检入的真实代码、配置、迁移脚本
2. 当前模块下的 `AGENTS.md`
3. 根目录 `AGENTS.md`
4. 与现有代码相匹配的设计文档（`docs/` 下的当前态文档）
5. 路线图、目标态文档、原型记录

## 已确认漂移
- 文档常写 `/api/v1/...`，当前后端控制器实际使用 `/api/...`
- 认证文档主要按邮箱登录描述，但当前后端实际按用户名登录
- `数据库设计.md` 等旧文档里仍有 MySQL 表述，当前真实数据库是 PostgreSQL
- 文档描述了 Web/iOS/admin 等能力，但当前仓库实际只有后端、Android、文档和 HTML 原型
- 同步、搜索、收藏、系统信息等设计明显领先于当前实现

## 高频入口
| 任务 | 位置 | 说明 |
|------|------|------|
| 开发主计划 | `docs/完整开发计划.md` | 新会话默认先看这里，确认阶段、优先级、依赖与接口级 TODO |
| Android 重启基线 | `docs/Android客户端Java重启基线.md` | 查看 Java 重启决策背景、历史冻结信息和当前实现边界 |
| 经验沉淀 | `docs/开发测试部署经验库.md` | 开发、联调、测试、部署前先看这里，避免重复踩坑 |
| 产品/需求 | `docs/需求分析.md` | 看目标、范围、状态标记 |
| 总体架构 | `docs/架构设计.md` | 区分当前态与目标态 |
| 当前 API | `docs/API接口设计.md` | 先看当前实现，再看未实现目标态 |
| 数据库事实 | `docs/数据库设计.md` | 与 Flyway 一致的当前态文档 |
| 同步设计 | `docs/数据同步协议设计.md` | 目标态优先，当前实现较轻 |
| 后端入口 | `thunder-note-server/src/main/java/com/flashnote/FlashNoteApplication.java` | Spring Boot 入口 |
| Android 入口 | `thunder-note-android/app/src/main/java/com/flashnote/java/MainActivity.java` | Java 客户端宿主 |
| Android 主壳 | `thunder-note-android/app/src/main/java/com/flashnote/java/ui/main/MainShellFragment.java` | 当前主页面与底部 tab |

## 约定
- `AGENTS.md` 一律使用中文编写。
- 与用户默认使用中文沟通，除非用户明确要求改用其他语言。
- 做任何结论前，先判断问题属于：当前实现、计划架构，还是原型/设计意图。
- 文档与代码不一致时，必须显式指出差异，不能静默选边。
- 后端默认遵循 controller → service → mapper/entity；统一响应结构是 `{ code, message, data, timestamp }`。
- 当前 Android Java 客户端以 `com.flashnote.java` 包为主，优先采用 Activity/Fragment + ViewBinding + ViewModel 的传统结构。
- 当前 Java 客户端里，`auth`、`flashnote`、`chat` 已有可编译主链；`collection`、`favorite`、`profile` 仍偏占位壳层。
- 判断 Android 是否为真实实现还是桩实现时，优先检查 repository 是否有真实网络调用、是否只返回内存数据或占位数据。
- 遇到 Android 相关任务时，优先区分：当前 Java 客户端事实、未来待补能力。
- git 提交时，根仓只提交文档、原型、脚本和规则类改动；`thunder-note-server/`、`thunder-note-android/` 的代码改动必须进入各自子仓库单独提交。
- 不要在根仓用 `git add -f` 把子仓代码快照纳入提交；若误纳入，必须在根仓回退索引并改为在子仓提交。

## 反模式
- 不要把路线图文档当作"模块已经存在"的证据。
- 不要因为文档定义了 Web 或 iOS，就默认仓库里已有对应代码。
- 不要在没有核对代码的情况下直接改文档去"对齐意图"，也不要在没有明确决策时直接改代码去"对齐文档"。
- 不要把 `artifacts/`、`build/`、`target/`、`node_modules/` 当成实现参考。

## 协作要求
- 启动新会话处理开发任务前，默认先阅读 `docs/完整开发计划.md`，确认当前阶段、优先级、依赖顺序，再开始实施。
- 遇到开发、测试、构建、部署类任务时，默认补读 `docs/开发测试部署经验库.md`，优先复用已有经验。
- 处理实现类任务时，先读相关文档，再回到代码核实后动手。
- 处理后端/API 任务时，必须先确认真实控制器路径与请求/响应结构。
- 处理 Android 任务时，必须先区分：当前 Java 客户端已实现部分、当前 Java 客户端占位部分；不能混写成一个统一成熟状态。
- 处理视觉/产品问题时，先看原型与 `docs/页面设计规范.md`，再确认当前代码是否真的实现。
- 如果一个判断会影响接口、数据结构或跨端行为，至少同时核对文档与一处真实代码入口。
- 遇到跨根仓 + 子仓的 git 操作时，先判断变更归属：文档/原型走根仓，后端代码走 `thunder-note-server/`，Android 代码走 `thunder-note-android/`；必要时三个仓分别提交。

## Git 提交规范
- **提交信息必须使用中文**，清晰描述本次改动的目的和内容。
- **按功能分次提交**，不要将多个不相关的改动混在一个提交中：
  - Bug 修复：一个提交只包含一个 bug 的修复
  - 功能开发：一个提交只包含一个独立功能
  - 重构：一个提交只包含一个模块的重构
  - 文档更新：可以单独提交，也可以与对应的代码改动一起提交
- **提交信息格式建议**：
  - `修复：登录后无法跳转到闪记列表页面` — bug 修复
  - `新增：调试日志功能，用于捕获完整异常堆栈` — 新功能
  - `优化：替换 requireContext() 为安全的 getContext()` — 代码优化
  - `文档：更新 AGENTS.md 添加 Git 提交规范` — 文档更新
- **提交前检查**：
  - 确认改动属于当前仓库（根仓 vs 子仓库）
  - 确认没有误提交 IDE 配置、build 产物等
  - 确认提交信息准确描述了改动内容
- **提交信息一律走文件，不要用多行 `-m`**：
  - 在 IDE 内置终端里用 `git commit -m "多行..."` 容易因 shell 等待引号闭合而把命令面板挂住，造成看似"卡死"的体验。
  - 标准做法：先 `write_to_file` 把提交信息写到 `/tmp/tn_<task>_commit_msg.txt`，再 `git commit -F /tmp/tn_<task>_commit_msg.txt`。
  - 单行简短提交可以用 `git commit -m "..."`，但消息中**不允许出现换行**。
  - 反模式：`git commit -m "第一行<换行>第二行"`，无论是否带 `\n`，都不要再生成。
- **严禁在 `run_command` 里用任何形式的多行输入写文件，包括但不限于：**
  - `cat > /tmp/foo.txt << 'EOF' ... EOF`（heredoc）
  - `printf '第一行\n第二行\n' > /tmp/foo.txt`（多行 printf）
  - 多行 `echo -e "..."`、`tee`、`> file` 接续多行字符串
  - 这些方式在 IDE 内置终端里**与 `git commit -m "多行..."` 等价**，shell 仍在等待 EOF/引号闭合，会把命令面板挂住，用户体验是"卡死"。即使命令在普通 shell 里能跑通，IDE 终端里也不能用。
- **写文件只走 `write_to_file` / `edit` 工具，不走 `run_command`**：
  - 创建提交信息文件、临时脚本、临时配置一律用 `write_to_file`。
  - 文件创建好之后再用 `run_command` 调 `git commit -F <path>` 或 `bash <path>`。
  - 如果上一次 `write_to_file` 写到 `/tmp/` 的文件因系统清理消失了，**重新调 `write_to_file`**，不要改用 heredoc"绕过"。
- **反模式锁定（多次重犯，强制锁死）：**
  - 出现 `<< 'EOF'`、`<<EOF`、`<<-EOF`、多行 `printf`、多行 `echo -e` 写文件 → 一律视为违规，必须立刻改成 `write_to_file`。
  - 出现 `git commit -m "<带换行的字符串>"` → 一律视为违规。
  - 检测到自己已经写出违规命令草稿，应当主动撤回并改用 `write_to_file`，而不是"先发出去试试"。

## 阶段开发原则
- 默认按 `docs/完整开发计划.md` 的阶段顺序推进：MVP → 增强版 → 完整项目 → 远期扩展。
- 除非任务明确要求跨阶段，否则当前阶段未闭环前，不要提前实现下一阶段能力。
- 当前阶段是否完成，以 `docs/完整开发计划.md` 中对应阶段的验收口径为准。

## 开发任务闭环规则（强制三关，不允许跳过）
每次开发任务，编写完代码后，必须依次完成：
1. **代码审查**：检查实现是否合理、是否符合既定约定、是否引入冗余或破坏既有结构、命名/分层/边界是否得当。
2. **安全评估**：检查是否引入鉴权/越权风险、是否泄露敏感信息（token、密钥、堆栈、内部字段）、是否对外开放未应放行的端点、是否有 XSS/CSRF/注入面、是否对客户端输入有防御。
3. **测试**：运行相关单元测试、集成测试或端到端验证。新增能力必须配套写测试或扩展既有测试，不得只靠手测。

判定规则：
- 三关任意一关不通过，必须先回到代码修改阶段，然后重新依次进行审查、评估、测试。
- 必须**全部三关都通过**，该任务才算开发完成，才能在 `docs/完整开发计划.md` 中标记完成、才能提交进入下一任务。
- 不允许"先提交，下个会话再补测试 / 再补审查"——补测试本身是上一个任务的延续，未补完前不视为已完成。
- 该规则适用于所有模块（后端、前端、Android、文档脚本类同样适用），不允许以"任务太小"为理由跳过。

## 实现完成后规则
- 完成接口、模型、模块或关键联调后，必须同步更新 `docs/完整开发计划.md` 中对应任务状态与说明。
- 如果实现改变了当前事实，还必须同步回写对应当前态文档，如 `API接口设计.md`、`数据库设计.md`、`客户端架构设计.md`、`架构设计.md`。
- 如果本次实现暴露了新的关键漂移或约束，应该补入相关 `AGENTS.md` 或文档的"差异/备注/维护规则"区块，避免下次会话重复踩坑。
- 如果本次开发、测试、构建、部署中遇到了值得复用的真实问题与解决方式，必须优先补入 `docs/开发测试部署经验库.md`。

## 常用命令
```bash
cd thunder-note-server && mvn clean compile
cd thunder-note-server && mvn test

cd thunder-note-android && ./gradlew assembleDebug
cd thunder-note-android && ./gradlew test
cd thunder-note-android && ./gradlew lint
```

## 备注
- "生成时间""仓库状态"是静态记录，仓库状态变化后需要手动更新。
- 当前未发现 CI 工作流。
- 后端虽然已有测试依赖，但几乎没有实际测试代码；Android 测试覆盖也仍偏占位。
