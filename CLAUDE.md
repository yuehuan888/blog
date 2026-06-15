# CLAUDE.md — GreenRead 博客平台（前后端 Monorepo）

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介

GreenRead — 类似小红书的公开社交内容平台。任何人可以注册、发布文章、点赞收藏、评论互动。前后端分离架构，共用一个 Git 仓库。

## 仓库结构

```
vibecoding1/
├── CLAUDE.md                  # 本文件 — 整体概览
├── blog-backend/              # Spring Boot 后端（详见 blog-backend/CLAUDE.md）
│   ├── src/main/java/com/blog/
│   ├── src/main/resources/
│   └── pom.xml
├── blog-frontend/             # Nuxt 3 前端（详见 blog-frontend/ 目录）
│   ├── pages/       # 11 个页面
│   ├── components/  # 8 个组件
│   ├── api/         # 请求封装 + 4 个模块
│   ├── stores/      # Pinia 状态管理
│   ├── middleware/  # 路由鉴权中间件
│   ├── types/       # TypeScript 类型定义
│   └── nuxt.config.ts
└── docs/                      # 设计文档和计划
    └── superpowers/
        ├── specs/             # 设计规范
        └── plans/             # 实施计划
```

## 快速启动

### 前置条件
- Java 17, Maven
- Node.js 18+, npm
- MySQL（需创建 `blogsystem` 数据库）
- Redis（需运行在 `localhost:6379`，热榜/缓存/去重依赖）

### 后端（blog-backend）

```bash
cd blog-backend
# 首次运行需建库：
# mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS blogsystem DEFAULT CHARSET utf8mb4;"
mvn spring-boot:run
# → http://localhost:8080
```

配置见 `blog-backend/src/main/resources/application.yml`（默认 MySQL root/123456，Redis localhost:6379）。

### 前端（blog-frontend）

```bash
cd blog-frontend
npm install
npm run dev
# → http://localhost:3000
```

前端通过 `nuxt.config.ts` 中 `runtimeConfig.public.apiBase` 直连后端 `http://localhost:8080`。

## 技术选型

| 层 | 后端 | 前端 |
|----|------|------|
| 框架 | Spring Boot 3.2.5 | Nuxt 3 |
| 语言 | Java 17 | TypeScript |
| ORM | MyBatis-Plus 3.5.6 | — |
| UI | — | Naive UI + Tailwind CSS |
| 状态 | — | Pinia |
| 构建 | Maven | Vite (Nuxt 内置) |
| 数据库 | MySQL (H2 测试) | — |
| 认证 | JWT | JWT (localStorage) |

## 鉴权规则

| 方法 | 要求 | 前端表现 |
|------|------|---------|
| GET `/api/**` | 可选 token | 公开浏览，登录后识别用户身份 |
| POST/PUT/PATCH/DELETE `/api/**` | 强制 token | 未登录跳转 /user/login |
| `/api/auth/**` | 无需 | 注册/登录/登出 |

### 权限模型
| 操作 | 权限 |
|------|------|
| 发表文章 | 登录即可 |
| 编辑/删除文章 | 作者本人 或 管理员 |
| 设置文章标签 | 作者本人 或 管理员 |
| 创建/编辑/删除标签 | 仅管理员 |
| 管理评论 | 仅管理员 |
| 浏览公开内容 | 无需登录 |

### 登录状态持久化
- JWT 存储在 localStorage，登录后即使刷新浏览器也保持登录态
- `plugins/auth-persist.client.ts`：Nuxt SSR hydration 后从 localStorage 恢复 token，解决服务端渲染覆盖问题
- 退出登录时清除 JWT、用户信息、本地草稿

## 关键业务逻辑

### 文章发布与草稿
- 写文章页：标签下拉多选（从管理员创建的标签中选择），第一个标签自动作为分类
- 草稿：保存到后端（status=draft），同时 localStorage 保留一份本地备份（key 按 userId 隔离，防止跨用户泄露）
- 个人主页：区分「发布的文章」和「草稿」两个区域，草稿仅自己可见
- 文章列表默认只返回 `status=published`

### 文章删除（级联清理）
删除文章时清除以下所有关联数据：
- article_tag（标签关联，并更新 tag.article_count）
- article_like / article_favorite（点赞/收藏）
- article_read（阅读记录）
- comment + comment_like（评论及评论点赞）
- article_history（版本历史）
- Redis 缓存（article 缓存、like/favorite Set、hot ZSET、categoryStats、tag cloud）

### 标签系统
- 管理员在 `/admin/tags` 创建标签 → 标签云立即可见
- 用户写文章时从已有标签中选择（最多 5 个）
- 首页标签栏从标签云取前 20 个展示，点击按标签筛选文章
- 标签云直接查库（不缓存），保证数据实时准确

### Redis 缓存策略

| 数据 | Key | 类型 | TTL |
|------|-----|------|-----|
| 文章详情 | `article::{id}` | String(JSON) | 30 min |
| 分类统计 | `categoryStats` | String(JSON) | 30 min |
| 点赞用户 | `article:like:{id}` | Set | 永不过期 |
| 收藏用户 | `article:favorite:{id}` | Set | 永不过期 |
| 阅读去重 | `article:read:{id}:{userId}` | String | 30 min |
| 7天热榜 | `article:hot:7` | ZSET | 2 h |
| 30天热榜 | `article:hot:30` | ZSET | 2 h |

缓存更新策略：写操作主动删除缓存，读操作懒重建（Cache-Aside 模式）。

## 设计系统（前端）

| Token | 值 | 用途 |
|-------|-----|------|
| Primary | #2D6A4F | 主色 |
| Primary Light | #52B788 | 渐变/hover |
| Background | #F0F7F4 | 页面底色 |
| Accent | #FF6B6B | 点赞/收藏 |

- 字体：PingFang SC / Microsoft YaHei
- 卡片圆角：12px
- 按钮圆角：8px
- 标签圆角：20px

## 前端路由总览

| 路由 | 页面 | 渲染 | 鉴权 |
|------|------|------|------|
| `/` | 首页 | CSR | 公开 |
| `/article/[id]` | 文章详情 | CSR | 公开 |
| `/article/write` | 写文章 | CSR | 登录 |
| `/article/hot` | 热门排行 | CSR | 公开 |
| `/tag/index` | 标签云 | CSR | 公开 |
| `/tag/[id]` | 标签筛选 | CSR | 公开 |
| `/user/[id]` | 个人主页 | CSR | 公开 |
| `/user/login` | 登录 | CSR | 访客 |
| `/user/register` | 注册 | CSR | 访客 |
| `/admin/tags` | 标签管理 | CSR | 管理员 |
| `/admin/comments` | 评论管理 | CSR | 管理员 |
