# GreenRead 博客平台 — Docker 部署指南

## 环境要求

- CentOS 7+ / Ubuntu 18+
- 内存 ≥ 4GB
- 安装 Docker + Docker Compose

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | bash
systemctl enable docker && systemctl start docker

# 安装 Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose
```

---

## 一、首次部署

### 1. 克隆代码

```bash
cd /opt
git clone <你的仓库地址> greenread
cd greenread
```

> ⚠️ 如果 GitHub 被墙，先配代理：`git config --global https.proxy http://192.168.88.1:7890`

### 2. 构建并启动

```bash
docker compose up -d --build
```

首次构建约 5-10 分钟。完成后访问 `http://<VM的IP>`。

> 就这么简单。宿主机只需装 Docker，不需要 Nginx/Node/Java 任何东西。

---

## 二、关键文件说明

| 文件 | 作用 |
|------|------|
| `docker-compose.yml` | 编排 5 个容器（Nginx + 前端 + 后端 + MySQL + Redis） |
| `blog-backend/Dockerfile` | 后端：Maven 编译 → JDK17 运行 |
| `blog-frontend/Dockerfile` | 前端：Node 编译 → Nitro 服务器 |
| `blog-frontend/.dockerignore` | 排除 `.env`，防止本地配置混入 |
| `nginx/nginx.conf` | Nginx 反向代理配置（容器内） |

## 三、Docker Compose 架构

```
                         ┌──────────────────────────────┐
                         │         Docker                │
                         │                              │
    外界 ←─ :80 ──→ ┌──────────┐                      │
                    │  Nginx   │                      │
                    │ (alpine) │                      │
                    └──┬───┬───┘                      │
                       │   │                           │
              ┌────────┘   └────────┐                  │
              ▼                     ▼                  │
     ┌──────────────┐    ┌──────────────────┐         │
     │ Frontend:3000│    │   Backend:8080   │         │
     │ (Nitro)      │    │  (Spring Boot)   │         │
     └──────────────┘    └──┬──────┬────────┘         │
                            │      │                   │
                   ┌────────┘      └────────┐          │
                   ▼                        ▼          │
          ┌──────────────┐         ┌──────────────┐   │
          │  MySQL :3306 │         │  Redis :6379 │   │
          └──────────────┘         └──────────────┘   │
                         └──────────────────────────────┘
```

## 四、日常运维

### 代码更新后重新部署

```bash
cd /opt/greenread
git pull

# 只改前端
docker compose up -d --build frontend nginx

# 只改后端
docker compose up -d --build backend

# 都改了
docker compose up -d --build
```

### 查看日志

```bash
docker logs greenread-backend --tail 50
docker logs greenread-frontend --tail 50
docker logs greenread-nginx --tail 50
```

### 重启服务

```bash
# 全部重启
docker compose restart

# 单个重启
docker compose restart frontend
```

### 进入容器调试

```bash
docker exec -it greenread-backend bash
docker exec -it greenread-mysql mysql -uroot -proot123 blogsystem
```

### 数据备份

```bash
# 备份数据库
docker exec greenread-mysql mysqldump -uroot -proot123 blogsystem > backup.sql

# 备份上传文件
docker cp greenread-backend:/app/uploads ./uploads-backup/
```

### 查看容器状态

```bash
docker compose ps
```

## 五、故障排查

### 网站打不开

```bash
# 1. 检查所有容器
docker compose ps                    # 五个都应该 Up

# 2. 检查前端
docker exec greenread-nginx curl -s http://frontend:3000/ | head -5

# 3. 检查后端
docker exec greenread-nginx curl -s http://backend:8080/api/articles?page=1 | head -20

# 4. 全过程
curl http://127.0.0.1/               # 应返回前端 HTML
```

### 图片上传后看不到

```bash
docker exec greenread-backend ls /app/uploads/articles/   # 确认文件存在
curl -I http://127.0.0.1/uploads/articles/xxx.jpg         # 应返回 200 image/jpeg
```

### 端口被占用

```bash
# 如果宿主机之前装过 Nginx，停止并禁用
systemctl stop nginx && systemctl disable nginx

# 或者直接清掉
docker compose down && docker compose up -d --build
```

### 彻底重建

```bash
docker compose down                  # 停止并删除容器
docker compose up -d --build         # 重新构建启动
# 注意：数据卷(mysql_data, uploads_data)不会丢失
```

## 六、Navicat 连接数据库

| 参数 | 值 |
|------|-----|
| 主机 | VM 的 IP（如 192.168.88.131） |
| 端口 | 3306 |
| 用户名 | root |
| 密码 | root123 |

> 注意：主机 IP 必须**手动输入**，不要复制粘贴。

## 七、Redis 客户端连接

| 参数 | 值 |
|------|-----|
| 主机 | VM 的 IP |
| 端口 | 6379 |
| 密码 | 无（留空） |

> 推荐客户端：[Another Redis Desktop Manager](https://github.com/qishibo/AnotherRedisDesktopManager/releases)
