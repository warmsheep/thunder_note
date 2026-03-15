---
name: deploy-postgres
description: 使用 Docker 部署 PostgreSQL 17 数据库并执行 Thunder Note 项目的 Flyway 初始化脚本。当用户要求"部署数据库"、"初始化数据库"、"启动 PostgreSQL"、"配置数据库环境"、"运行数据库迁移"时使用此 skill。支持自动检测端口冲突并切换备用端口。
---

# Thunder Note PostgreSQL 部署

此 skill 用于快速部署项目所需的 PostgreSQL 数据库环境。

## 部署规格

| 配置项 | 值 |
|--------|-----|
| 容器名 | `thunder-db` |
| 镜像 | `postgres:17` |
| 数据库 | `thunder_note` |
| 用户 | `postgres` |
| 密码 | `postgres` |
| 默认端口 | `5432`（若被占用自动切换到 `15432`） |

## 执行步骤

### 1. 检查现有容器

```bash
docker ps -a --format "{{.Names}}" | grep -q "thunder-db"
```

如果容器已存在：
- 运行中 → 提示用户容器已在运行
- 已停止 → 询问是否启动或重建

### 2. 检查端口占用

```bash
ss -tlnp | grep -q ":5432"
```

- 端口空闲 → 使用 5432
- 端口占用 → 使用 15432 并提醒用户修改 `application.yml`

### 3. 启动容器

```bash
docker run -d \
  --name thunder-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=thunder_note \
  -p <PORT>:5432 \
  postgres:17
```

### 4. 等待就绪

```bash
for i in {1..10}; do
  docker exec thunder-db pg_isready -U postgres && break
  sleep 1
done
```

### 5. 执行初始化脚本

按顺序执行 Flyway 迁移脚本：

```bash
docker exec -i thunder-db psql -U postgres -d thunder_note < thunder-note-server/src/main/resources/db/migration/V1__initial_schema.sql
docker exec -i thunder-db psql -U postgres -d thunder_note < thunder-note-server/src/main/resources/db/migration/V2__add_flash_note_id_and_role_to_messages.sql
```

### 6. 验证部署

```bash
docker exec thunder-db psql -U postgres -d thunder_note -c "\dt"
```

应显示 5 张表：`users`, `user_profiles`, `flash_notes`, `messages`, `note_collections`

## 常用操作

### 连接数据库

```bash
# 通过 Docker
docker exec -it thunder-db psql -U postgres -d thunder_note

# 远程连接字符串
jdbc:postgresql://localhost:<PORT>/thunder_note
```

### 停止容器

```bash
docker stop thunder-db
```

### 删除容器（会丢失数据）

```bash
docker rm -f thunder-db
```

### 查看日志

```bash
docker logs thunder-db
```

## 注意事项

1. **端口冲突**：如果本地已有 PostgreSQL 服务占用 5432，部署会自动使用 15432 端口。此时需要修改 `thunder-note-server/src/main/resources/application.yml` 中的数据库连接端口。

2. **数据持久化**：当前配置未挂载数据卷，容器删除后数据会丢失。生产环境建议添加 `-v thunder-db-data:/var/lib/postgresql/data`。

3. **与 Spring Boot 联调**：部署完成后，确保 `application.yml` 中的端口与实际部署端口一致。
