# Thunder Note Web 构建与部署

> **建立时间：** 2026-05-09  
> **目标：** 同一个 Spring Boot jar 同时承载 Android 客户端 API 与 Web 用户端 SPA。  
> **覆盖：** 本地开发、生产构建、jar 部署、静态资源更新流程。

---

## 1. 工程结构

```
thunder-note-server/
├── pom.xml                        # 后端 Maven 工程
├── frontend/                      # Web 前端（Vue 3 + Vite + Pinia）
│   ├── package.json
│   ├── vite.config.js             # 构建产物输出到 ../src/main/resources/static/web/
│   └── src/
└── src/
    ├── main/
    │   ├── java/.../FlashNoteApplication.java
    │   ├── resources/
    │   │   ├── application.yml
    │   │   ├── db/migration/      # Flyway
    │   │   └── static/web/        # ← Vite build 产物，被 jar 打包
    │   └── ...
    └── test/
```

**事实：** `frontend/` 是后端工程的子目录而非独立仓库。两者用同一个 `git submodule`（`thunder-note-server/`）管理代码。Web 构建产物直接落到后端的 classpath 资源里。

---

## 2. 本地开发

### 2.1 中间件

```
docker compose up -d postgres redis minio minio-init
```

确认（在 docker / 应用层都看得到）：
- PostgreSQL `localhost:15432`（注意非默认 5432）
- Redis `localhost:6379`
- MinIO console `localhost:9001`，bucket `thunder-note`

> 历史踩坑见 `开发测试部署经验库.md`：本机已有 `postgres:18` 时不要再拉 `postgres:17`，可直接复用 `postgres:18` + 新卷 `thunder-note-postgres18-data`。

### 2.2 启动后端

```
cd thunder-note-server
mvn clean compile        # 顺带触发 Flyway 迁移到 V<最新>
java -jar target/thunder-note-server-*.jar
# 或 IDE 启动 com.flashnote.FlashNoteApplication
```

后端默认 `:8080`，提供：
- `/api/**` Android + Web 共用 API
- `/`, `/index.html`, `/assets/**`：Web SPA 静态资源
- `/{spa-route}`：SPA 内部路由（fallback 到 `/index.html`）
- `/actuator/health`：健康检查（permitAll）

### 2.3 前端两种开发模式

#### 模式 A：Vite 独立 dev server（**推荐，hot reload**）

```
cd thunder-note-server/frontend
npm install
npm run dev               # http://localhost:5173
```

`vite.config.js` 已配置 `/api` 代理到 `:8080`，前后端解耦开发，改前端代码立刻热更新，不影响后端。

#### 模式 B：构建产物嵌入后端 jar（**接近生产**）

```
cd thunder-note-server/frontend
npm run build             # 输出到 ../src/main/resources/static/web/
                          # 同时同步到 ../target/classes/static/web/
```

然后只需重启后端就能在 `:8080/` 看到最新 SPA。

> **关键：** `vite.config.js` 加了一个 `syncToTargetClasses` 插件，build 后会自动把 `static/web/` 增量同步到 `target/classes/static/web/`。这是因为 IDE 的 incremental sync 偶尔不可靠，会出现"build 完了但启动报 'static/web/index.html 未找到'"。详见 `开发测试部署经验库.md` 经验 73。

---

## 3. 生产构建

### 3.1 一行打 jar

```
cd thunder-note-server
cd frontend && npm install && npm run build && cd ..
mvn clean package -DskipTests=false   # 默认就会打前端构建产物（已落到 static/web/）
```

最终产物 `target/thunder-note-server-<version>.jar`，里面同时包含：
- 后端 controller / service / mapper / Flyway 迁移
- Web SPA 完整资源（`BOOT-INF/classes/static/web/`）

### 3.2 一行部署

```
java -jar target/thunder-note-server-*.jar \
  --spring.profiles.active=prod \
  --server.port=8080
```

或写入 systemd / k8s deployment / 容器镜像；这里不做容器化范本，按各自基础设施补。

---

## 4. 静态资源更新流程（仅前端改了，不动 controller）

```
cd thunder-note-server/frontend
npm run build
```

→ Vite 输出到 `../src/main/resources/static/web/`  
→ Vite 插件同步到 `../target/classes/static/web/`

之后两条路径任选其一：
- **重启后端**（生产、CI/CD 常规流程）
- **不重启** + 让 Spring Boot devtools 触发 reload（仅本地开发，依赖 devtools）

> **不要**手动删 `static/web/` 子文件，让 Vite `emptyOutDir: true` 与同步插件自己管理；手动操作会留下旧的 hash 文件名导致旧版页面卡在缓存里。

---

## 5. 安全边界

| 路径 | 是否需要鉴权 | 说明 |
|------|-------------|------|
| `/` `/index.html` `/assets/**` `/favicon.ico` | ❌ 公开 | Web SPA 入口 |
| `/{spa-route}`（如 `/login` `/notes`） | ❌ 公开 | fallback 到 `/index.html` |
| `/api/auth/login` `/api/auth/register` `/api/auth/refresh` | ❌ 公开 | 登录态尚未建立 |
| `/actuator/health` | ❌ 公开 | 健康检查 |
| `/api/**`（其他） | ✅ 鉴权 | 必须带 JWT |
| `/actuator/**`（其他） | ✅ 鉴权 | 监控端点 |

边界由 `common/config/SecurityConfig.java` 与 `common/web/WebStaticResourceConfig.java` 强制；自动测试 `WebStaticResourceIntegrationTest`（11 项）+ `ControllerHttpMethodContractTest`（10 项）守关，详见 `Web手工回归清单.md` § 12。

---

## 6. 数据库迁移

- Flyway 脚本在 `src/main/resources/db/migration/V*__*.sql`，按版本号自然顺序应用。
- 应用启动时自动执行新增的迁移；**不会**自动回滚。
- 新增字段或表必须先写 Flyway 迁移，再改 entity / mapper / service / `docs/数据库设计.md`，避免出现"代码引用不存在的列"启动报错。

---

## 7. 文件存储

- MinIO bucket：`thunder-note`（默认）
- 上传走 `POST /api/files/upload`（multipart）→ 返回 `objectName`
- 下载走 `GET /api/files/download?objectName=...` → 后端鉴权后从 MinIO 拉流回写
- Web 端用 `apiClient` 拉 blob 然后 `URL.createObjectURL`，组件 unmount 时 `revokeObjectURL` 释放

> 注意：Axios 的 `apiClient` 在 W17 已**移除**默认 `Content-Type: application/json`，避免 multipart 上传被覆盖。任何新增上传场景不要给 `FormData` 请求显式设置 `Content-Type`，让浏览器自动带 boundary。

---

## 8. 版本号 / 缓存破坏

- Vite 构建产物文件名带 hash（如 `index-CxarfFgn.js`），自动破坏浏览器缓存。
- `index.html` 没有 hash，但 Spring Boot 默认 `Cache-Control: no-cache`，每次都会重新拉。
- 部署后用户**无需手动刷新**就能看到新版本；如有顽固缓存可用强制刷新（Cmd+Shift+R）。

---

## 9. CI / 自动验证

> 当前**没有**配置 CI 工作流（GitHub Actions / GitLab CI），所有验证靠开发者本地执行。

推荐在 PR 合并前本地跑：

```
cd thunder-note-server/frontend && npm test          # 269 项 Vitest
cd thunder-note-server && mvn test                   # 90+ 项后端测试（含 W13-02 / W13-03）
cd thunder-note-server/frontend && npm run build     # 前端 build 不报错
cd thunder-note-server && mvn package -DskipTests    # 后端打包不报错
```

待补：建立 `.github/workflows/ci.yml`（`docs/完整开发计划.md` § C5 工程化与发布能力，远期项）。

---

## 10. 常见问题

| 现象 | 排查 | 处理 |
|------|------|------|
| 启动报"静态资源未找到: index.html" | `target/classes/static/web/` 是否完整 | `cd frontend && npm run build`（Vite 同步插件自动复制到 target） |
| `/api` 调用 404 | 路径是否漂移 | 跑 `mvn test -Dtest=ControllerHttpMethodContractTest` 确认 |
| 上传文件报"Current request is not a multipart" | apiClient 是否给 FormData 设了 Content-Type | 不要在请求里手动设 Content-Type，让 axios 自动加 boundary |
| 浏览器一刷新就刷出 ERROR 日志 | `/favicon.ico` 缺失被当 500 处理 | 已在 GlobalExceptionHandler 中把 NoResourceFoundException 收口为 404 + DEBUG，不必再处理 |
| Vite hot reload 改了不生效 | 是否运行的是 jar 内嵌资源 | 改用 `npm run dev`（:5173）开发，不要直接刷 :8080 |

---

## 11. 维护规则

- 修改本文件时，必须同时确认 `docs/开发测试部署经验库.md` 是否需要补/改对应经验项。
- 新增静态资源处理 / 安全边界规则 → 必须先在 `WebStaticResourceIntegrationTest` 中加测试再实现。
- 新增 controller 端点 → 必须在 `ControllerHttpMethodContractTest` 中固化路径 + HTTP 方法，再回写 `docs/API接口设计.md`。
