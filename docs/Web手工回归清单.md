# Thunder Note Web v1 手工回归清单

> **建立时间：** 2026-05-09  
> **对应版本：** D1 Web v1（W0–W19 + W12 + W13）  
> **执行频率：** 每次发版前必跑；CI 自动测试覆盖 controller / store / composable，但 UI 层最终行为由这份清单守关。  
> **维护规则：** 新增功能 / 修复 UI bug 时，必须把对应场景补到本清单；不能仅依赖自动测试。

---

## 0. 准备

```
后端：
  cd thunder-note-server && mvn clean compile
  docker compose up -d postgres redis minio minio-init   # 或本地手动启动
  java -jar target/thunder-note-server-*.jar             # 或 IDE 启动 FlashNoteApplication

前端构建产物已经被 mvn package 打进 jar 里。
本地开发可单独跑：
  cd thunder-note-server/frontend && npm run dev   # http://localhost:5173 + 代理 /api → :8080
```

回归测试推荐顺序：注册 → 登录 → 闪记 → 消息 → 文件 → 收藏 → 合集 → 搜索 → 联系人 → 修改密码 → 资料 → 登出。

---

## 1. 认证

### 1.1 注册（`/register`）
- [ ] 用户名 < 3 位 → 前端阻止提交并提示
- [ ] 密码 < 6 位 → 前端阻止提交
- [ ] 用户名重复 → 后端返回 4xx，toast 显示后端错误信息
- [ ] 注册成功 → 自动跳到 `/login`

### 1.2 登录（`/login`）
- [ ] 错误用户名 / 密码 → toast "用户名或密码错误"
- [ ] 正确登录 → 跳转 `/notes`，topbar 显示当前用户头像
- [ ] 浏览器**回车**键直接提交（form `@submit.prevent` 标准行为）
- [ ] 刷新页面后仍保持登录态（access token 写入 localStorage / sessionStorage）

### 1.3 Token 续期
- [ ] access token 过期后下一次 API 调用 → axios 拦截 401 → 自动 refresh → 重发原请求
- [ ] refresh token 也过期 → 跳回 `/login` 并清空登录态

### 1.4 登出
- [ ] 点击右上头像 → "退出登录" → toast 成功 → 跳到 `/login`
- [ ] 登出后强制访问 `/notes` → 自动跳 `/login`

---

## 2. 闪记（`/notes`）

### 2.1 列表
- [ ] 收集箱（`flashNoteId=-1`）固定置顶展示
- [ ] 普通闪记按 `updatedAt` 倒序
- [ ] 置顶闪记 📌 标识 + 在普通项前
- [ ] 隐藏闪记不出现在主列表中

### 2.2 创建 / 编辑 / 删除
- [ ] "+ 新建闪记" → 输入标题 + 选 emoji icon → 保存 → 列表出现新项
- [ ] 编辑闪记 → 标题与 icon 改后立即更新
- [ ] 删除闪记 → ConfirmDialog 二次确认 → 删除成功 toast
- [ ] 置顶 / 取消置顶 → 列表位置变化
- [ ] 隐藏 / 取消隐藏 → 切到 / 离开主列表

### 2.3 快速捕获 FAB（W22-01 / W22-02 / W22-05）
- [ ] 右下角悬浮「+」按钮，点击弹出气泡菜单
- [ ] 桌面端菜单项：文字 / 图片 / 视频 / 文件
- [ ] 移动端（pointer: coarse）额外有「拍照」一项
- [ ] 文字 → 弹出 `QuickCaptureDialog`：textarea + Cmd/Ctrl+Enter 直发 + ESC 取消；成功后 toast「已发送到收集箱」
- [ ] 图片 / 视频 / 文件 → 选择本地文件 → 直接发到收集箱；不打开会话页，闪记列表中收集箱预览自动刷新
- [ ] 移动端「拍照」 → 调起系统相机（input capture=environment）

---

## 3. 消息（`/chat/:flashNoteId` 与 `/chat/contact/:peerUserId`）

> **W20** 同一个 ChatView 同时承担两种身份：闪记会话 / 联系人 1v1 对话。下面 § 3.1～§ 3.7 在两种身份下都跑一遍；§ 3.8 是联系人模式专属；§ 3.9 / § 3.10 是 W22 创作能力。


### 3.1 进入会话（W19-02）
- [ ] 首次进入 → 自动滚到底部，看到最新消息
- [ ] 上滑后退出 → 重新进入 → 自动恢复到上次位置（sessionStorage）
- [ ] 关闭浏览器后再打开（清 sessionStorage） → 自动滚到底部

### 3.2 发送消息
- [ ] 输入文本 + 回车 / 点击发送 → optimistic 气泡立即显示（pending 状态）
- [ ] server 返回后气泡变实状态
- [ ] 网络断开 → 气泡变红 + "重试"按钮
- [ ] markdown 渲染：`**粗体** *斜体* [链接](https://example.com) - 列表` 都正确显示，链接安全（DOMPurify 净化）
- [ ] 发送 / 合并卡片成功后**平滑**滚到底部（W19-03）

### 3.3 加载历史（W19-03）
- [ ] 滚到顶部 → 自动加载更老一页 → 加载后**视觉位置不跳**（W19-03 anchor 修正）
- [ ] 没有更多历史时不再触发

### 3.4 新消息到达（W19-01 / W19-03）
- [ ] 用户在底部时新消息 → 自动跟随到底
- [ ] 用户上滑后新消息到达 → 屏幕底部出现「⬇ 新消息」浮动按钮
- [ ] 点击「⬇ 新消息」按钮 → 平滑滚到底，按钮消失

### 3.5 单条上下文菜单（W21）
- [ ] 鼠标右键消息气泡 → 弹菜单：复制 / 转发 / 收藏 / 多选 / 删除
- [ ] 触摸端长按 ≥600ms → 同样弹菜单（移动手指超过 8px 取消）
- [ ] ESC / 点空白 / 选中某项 → 菜单关闭
- [ ] 「复制」 → 文本到剪贴板 + toast「已复制」（卡片复制 title，媒体占位复制 fileName）
- [ ] 「转发」 → 转发对话框（mode='single'，描述文字"将这条消息转发"）
- [ ] 「收藏」 → 切换收藏状态；已收藏时菜单显示"取消收藏"
- [ ] 「多选」 → 进入多选模式并预选中该条
- [ ] 「删除」 → ConfirmDialog 二次确认
- [ ] 气泡永久按钮收敛：仅 failed 状态保留「重试」；favorited 时只显示一颗 ★ 标记，无独立按钮
- [ ] optimistic pending / failed 消息（无服务端 ID）不弹菜单

### 3.6 多选 / 删除 / 合并 / 转发（W17）
- [ ] 进入选择模式 → 多选消息
- [ ] 删除 → ConfirmDialog → 批量删除成功
- [ ] 合并 → 输入卡片标题 → 生成卡片消息（payload.cardType = COMPOSITE）
- [ ] 转发 → 选择目标闪记 → 消息已复制
- [ ] 点击卡片消息 → 弹窗显示卡片内 items 列表

### 3.7 收集箱清空（W16）
- [ ] 仅在 `flashNoteId=-1` 时显示「清空」按钮
- [ ] 清空后消息列表为空，闪记列表中收集箱 latestMessage 也更新
- [ ] 联系人对话中**不显示**「清空」按钮（W20-03 isInbox getter 在 peer 模式总为 false）

### 3.8 联系人 1v1 对话专项（W20）
- [ ] 从 `/contacts` → 点击 FRIEND 联系人「💬 聊天」 → 跳到 `/chat/contact/<userId>`
- [ ] 路由直进场景（粘贴 URL）→ ChatView 仍能拉消息；header 显示对方昵称（contactsStore 异步加载完成后会更新）
- [ ] header 头像：联系人有头像时使用 `AuthenticatedAvatar`；没有头像走 fallback 字
- [ ] 发送：optimistic 气泡的 `receiverId === peerUserId`，`flashNoteId == null`；server 回包后被替换
- [ ] 加载更多：上滑触发 loadMore，请求 body 含 `peerUserId`，**不**含 `flashNoteId`
- [ ] sessionStorage key 为 `tn:chat:scroll:peer:<userId>`，与 `tn:chat:scroll:fn:<userId>` 互不污染（W20-04）
- [ ] 多选 → 转发对话框：tab「闪记会话 / 联系人」可切换；联系人 tab 仅列出 `relationStatus === 'FRIEND'` 且非自己
- [ ] 收藏 / 删除 / 多选 / 合并卡片在联系人模式同样可用（mergeMessages 走 receiverId，sendMessage 走 receiverId）
- [ ] header「返回」按钮 fallback 走 `/contacts`（在闪记模式 fallback 走 `/notes`）

### 3.9 多媒体卡片新建（W22-03）
- [ ] header 「📇 新卡片」按钮（仅非多选时显示）
- [ ] 弹出 `CardEditorDialog`：标题（必填，maxLength 50）+ 正文（可选）+ 多附件区（max 9）
- [ ] 「+ 添加附件」可选多文件；附件溢出 9 个时提示
- [ ] 拖拽文件到对话框任意位置 → 加入附件；拖拽时显示半透明覆盖层
- [ ] 在正文 textarea 粘贴图片 → 自动加入附件
- [ ] 单类型 image → 后端自动判 cardType=IMAGE_COLLECTION；单类型 video → VIDEO_COLLECTION；单类型 file → FILE_COLLECTION；混合类型 → COMPOSITE_CARD
- [ ] 保存：依次串行上传，每个附件显示当前进度；全部成功后单条 COMPOSITE 消息进会话，非中间多条单消息
- [ ] toast「卡片已创建」，列表自动 reload 拉回新卡片
- [ ] 联系人 1v1 模式同样可用（receiverId 路径）

### 3.10 Composer 多附件 + 粘贴 + 拖拽（W22-04 / W22-06 / W22-07）
- [ ] 「📎」按钮可一次选择多文件（multiple）；图片/视频显示缩略图，文件显示 emoji
- [ ] 单个附件可独立移除；附件累计达 9 个时新增触发 toast 提示
- [ ] textarea 粘贴图片 → 直接进入附件队列（preventDefault 阻止默认粘贴文本）
- [ ] 整个 composer 支持拖拽文件 → 进入附件队列；拖拽时显示「松手上传」提示
- [ ] 含 N 个附件时发送：第一条带文本（caption），后续 N-1 条只带 media；进度条按文件切换
- [ ] 任一附件上传失败：剩余文件保留在 pendingFiles，toast 显示「已发送 X/Y」
- [ ] 移动端 composer 出现「📷」按钮 → 调系统相机；桌面端不显示该按钮

---

## 4. 文件与媒体（W9 + W18）

### 4.1 图片
- [ ] 上传 .jpg / .png / .gif / .webp → 气泡显示缩略图
- [ ] 缩略图保持原始宽高比，不被拉伸（FavoritesView 同）
- [ ] 点击缩略图 → 全屏 lightbox（W18-01）
- [ ] lightbox：滚轮 / +/- 缩放、拖动平移、ESC 关闭

### 4.2 视频
- [ ] 上传 .mp4 → 气泡显示视频，可原位播放
- [ ] hover 时右上角「全屏」按钮 → iOS Safari `webkitEnterFullscreen` / 桌面 `requestFullscreen`（W18-03）

### 4.3 音频
- [ ] 上传 .mp3 / .m4a → 气泡显示音频播放器，可播放

### 4.4 PDF（W18-02）
- [ ] 上传 .pdf → 气泡显示文件卡片 + 「预览」+「下载」按钮
- [ ] 「预览」 → iframe 全屏内嵌渲染（浏览器原生 PDF 阅读器）
- [ ] 关闭后 blob URL 释放

### 4.5 文本文件
- [ ] 上传 .txt / .md / .json / .log → 气泡显示文件卡片 + 「预览」按钮
- [ ] 「预览」 → 全屏文本对话框 UTF-8 解码内容

### 4.6 Office 文件
- [ ] 上传 .docx / .xlsx / .pptx → 气泡显示文件卡片 + 「下载」（无在线预览）
- [ ] 显示提示「暂不支持在线预览，请下载后查看」

### 4.7 上传约束
- [ ] 上传无效类型（如 .exe）→ 后端拒绝
- [ ] 上传 > 20MB → 后端拒绝

---

## 5. 收藏（`/favorites`）

- [ ] 在消息气泡上点 ⭐ → 加入收藏，气泡 ⭐ 变实
- [ ] `/favorites` 列出已收藏消息
- [ ] **媒体收藏**：图片 / 视频 / PDF / 文件卡片正确预览，宽高比保持
- [ ] **文本收藏**：markdown 渲染（**加粗**、链接 等）
- [ ] **卡片收藏**：仅显示 `[卡片] 标题` 摘要
- [ ] 点击收藏项 → 跳到原闪记 `/chat/<flashNoteId>`

### 5.1 上下文菜单（W21-05）
- [ ] 鼠标右键收藏项 → 菜单：复制 / 取消收藏
- [ ] 触摸长按 ≥600ms → 同样弹菜单
- [ ] 「复制」 → 文本到剪贴板 + toast「已复制」（卡片复制 title，媒体占位复制 fileName）
- [ ] 「取消收藏」 → 列表移除
- [ ] 永久按钮收敛：右下角不再有「取消收藏」按钮，仅显示提示文字「· 右键/长按菜单取消收藏」

---

## 6. 合集（`/collections`）

- [ ] 列表展示所有合集（按 `flash_notes.tags`）
- [ ] 创建合集 → 选闪记 → 保存
- [ ] 编辑合集 → 改标题 / 闪记成员
- [ ] 删除合集 → ConfirmDialog
- [ ] 点击合集 → 展开闪记列表 → 点击进入会话

---

## 7. 搜索（`/search`，W12-03 改造）

- [ ] 聚焦输入框 + 输入关键词 + **Enter** → 触发搜索（form `@submit.prevent` 标准行为）
- [ ] 屏幕阅读器读到 `role="search"` 与 sr-only label
- [ ] 结果分两组：闪记名称命中 / 消息内容命中
- [ ] 点击闪记结果 → `/chat/<flashNoteId>`
- [ ] 点击消息结果 → `/chat/<flashNoteId>` 定位到消息所在会话
- [ ] 搜索期间快速切换关键词 → 后到结果不会被旧请求覆盖（runId 守卫）
- [ ] 「×」清空 → 输入框清空 + 焦点回到输入框

---

## 8. 联系人（`/contacts`，W14）

- [ ] 标签页：联系人 / 待处理（pending count badge）
- [ ] 联系人列表：点击 → 进入与该用户的会话（如已有）
- [ ] 添加：搜索用户名 → 选中 → 发送好友请求
- [ ] 待处理：接受 → 双向加入联系人；拒绝 → 列表移除
- [ ] 取消已发出请求 → 对方待处理列表也消失
- [ ] 删除联系人 → ConfirmDialog → 双向删除

---

## 9. 修改密码（`/change-password`，W15）

- [ ] 三个字段：当前密码 / 新密码 / 确认新密码
- [ ] 新密码 < 6 位 → 前端阻止
- [ ] 两次新密码不一致 → 前端阻止
- [ ] 当前密码错 → 后端返回 4xx → toast
- [ ] 修改成功 → toast，但旧 token 仍有效到过期

---

## 10. 个人资料（`/profile`，W11 + W23）

- [ ] 头像 / 昵称 / 简介 显示当前数据
- [ ] 编辑昵称（≤32 字）+ 简介 → 保存 → 立即生效
- [ ] 「我的联系人」入口 → `/contacts`
- [ ] 「修改密码」入口 → `/change-password`
- [ ] 「⚙ 设置」入口 → `/settings`（W23-03）
- [ ] 「退出登录」 → 清空 store + 跳 `/login`
- [ ] **不显示** access_token / refresh_token

### 10.1 头像选择与裁剪（W23-01 / W23-02）
- [ ] 点击「更换头像」 → 弹出 `AvatarPickerDialog`，默认选中 Emoji tab
- [ ] Emoji tab：4×3 网格，12 个表情（💼 📚 ❤️ 🌟 🎯 🚀 🎨 🎵 📷 🍕 ⚽ 😊）
- [ ] 当前头像若为 emoji，对应格子高亮
- [ ] 点击任一 emoji → 关闭对话框 + toast「头像已更新」+ 头像区直接展示 emoji（不走网络）
- [ ] 切到「上传图片」tab → 选本地图片 (image/* 且 ≤5MB)
- [ ] 非图片 / 过大 → 提示错误不进入裁剪
- [ ] 选图合法 → 自动关闭 picker，弹出 `AvatarCropDialog`
- [ ] 裁剪：拖动图片 / 滚轮缩放 / 底部滑块 / ±按钮 缩放；圆形遮罩预览
- [ ] 确认 → 输出 512×512 JPEG 上传 → toast「头像已更新」
- [ ] 取消裁剪 → 返回 picker（不是直接关闭）
- [ ] 上传中「更换头像」按钮显示进度百分比并禁用

### 10.2 统计卡片（W23-05）
- [ ] 三列网格：闪记 / 消息 / 收藏
- [ ] 数据未加载时显示「-」；加载成功显示实际数字
- [ ] 数据来源：闪记 = flashNotesStore.visibleCount；消息 = `/api/messages/count`；收藏 = favoritesStore.list.length

---

## 11. 设置（`/settings`，W23-03）

- [ ] header 返回按钮 → 返回上级（优先浏览器 back，否则 `/profile`）
- [ ] 通用块：
  - [ ] 「修改密码」 link-row → `/change-password`
  - [ ] 「关于 Thunder Note」 link-row → `/settings/about`
- [ ] 系统信息块：服务地址 / 客户端版本 / 构建时间（仅 env 有值时）/ 当前用户 ID / 用户名
- [ ] 项目块：项目主页外链（target=_blank + rel=noopener）
- [ ] 危险块：「退出登录」 → 清空 store + 跳 `/login`
- [ ] 不显示任何 token

---

## 12. 关于（`/settings/about`，W23-04）

- [ ] header 返回 → 返回上级（优先浏览器 back，否则 `/settings`）
- [ ] Hero 卡片：应用图标 / 应用名「Thunder Note 闪记」/ 简介 / 版本号（VITE_APP_VERSION）/ 构建时间（VITE_APP_BUILD_TIME，可选）
- [ ] 项目主页外链
- [ ] Web 技术栈（5 项：Vue 3 / Vite / Pinia / Vue Router / marked+DOMPurify），每项都能点开到原项目
- [ ] 致谢
- [ ] 许可证说明段

---

## 13. 响应式与可用性（W12）

### 13.1 桌面（W12-01）
- [ ] 1440 / 1920：sidebar 248px，content 居中限宽 1180px
- [ ] 4K：内容不会撑满整行
- [ ] ChatView 在 ≥1280：消息列宽视觉收窄到 980px 居中，header / composer 全宽

### 13.2 移动（W12-02）
- [ ] ≤768：sidebar 收起 → bottombar；topbar user-name 隐藏（保留头像 + 红点）
- [ ] ≤480：bottombar 字号收紧；content padding 收紧；ChatView padding 12px
- [ ] iOS Safari：`env(safe-area-inset-bottom)` 适配底部小白条
- [ ] 输入框聚焦不会触发整页缩放（`-webkit-text-size-adjust: 100%`）

### 13.3 a11y（W12-03 / W12-04）
- [ ] **键盘 Tab**：所有按钮 / 输入框 / 链接显示**绿色焦点环**
- [ ] **鼠标点击**：不显示焦点环（`:focus` vs `:focus-visible`）
- [ ] 屏幕阅读器：搜索框 / 清空按钮 / 头像有可读 label
- [ ] ESC：ImageLightbox / PdfViewerDialog / TextViewerDialog 都关闭
- [ ] ::selection 显示主色背景
- [ ] ::placeholder 颜色一致

---

## 14. 安全边界（自动测试覆盖）

> 这些都已被 `WebStaticResourceIntegrationTest` 与 `ControllerHttpMethodContractTest` 自动校验，列在这里仅作清单。如自动测试通过则视为这一节通过。

- [ ] `/` → SPA index.html
- [ ] `/{spa-route}` (如 `/login` `/notes`) → fallback 到 SPA
- [ ] `/assets/missing.js` → HTTP 404（不是 SPA index）
- [ ] `/api/messages/list`（无 token）→ 401
- [ ] `/api/auth/login` → 路由到 controller，不会被 SPA 吞
- [ ] `/api/some-non-existent-endpoint` → 401（不会泄漏 SPA index）
- [ ] `/favicon.ico` → 404（不是 500 / 不是 SPA index）

---

## 13. 故障演练（可选）

- [ ] 杀掉 Redis：登录可继续（仅 token 黑名单不可用）
- [ ] 杀掉 MinIO：上传报错有提示，文本消息仍可发
- [ ] 杀掉 PostgreSQL：所有 API 报 5xx；前端 toast；不崩页
- [ ] 离线模式：发送消息 → optimistic 气泡 + 重试按钮，不丢失输入

---

## 维护

每次发版前：
1. 执行 `cd thunder-note-server/frontend && npm test`（应 269 项全过）
2. 执行 `cd thunder-note-server && mvn test`（应 90 项全过：86 + 4 W13 新增契约）
3. 执行本清单 → 任何 ❌ 都必须在发版前修复
4. 发版后把执行结果归档，便于排查回归来源
