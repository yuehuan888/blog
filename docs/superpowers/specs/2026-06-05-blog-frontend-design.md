# Blog Frontend Design Specification

**Date:** 2026-06-05
**Status:** Approved
**Backend:** blog-backend (Spring Boot + 28 REST endpoints)

## Overview

为现有的博客后端系统构建完整前端，定位为公开社交内容平台（类似小红书），每个人都可以注册、发布文章、互动。采用 Nuxt 3 实现 SSR/SSG 混合渲染，确保 SEO 友好。

## Tech Stack

| 维度 | 选型 | 理由 |
|------|------|------|
| Framework | Nuxt 3 | SSR/SSG 混合渲染，公开内容需 SEO |
| Language | TypeScript | 类型安全 |
| UI Library | Naive UI | 设计感强，组件齐全，适合年轻化产品 |
| CSS | Tailwind CSS | 补充样式，卡片/布局微调 |
| State | Pinia | 官方推荐，SSR 友好 |
| HTTP | $fetch (Nuxt built-in) | 原生 SSR 支持，自动处理 cookie |

## Design System

### Color Palette — Forest Green

| Token | Value | Usage |
|-------|-------|-------|
| Primary | #2D6A4F | 主按钮、链接、导航高亮 |
| Primary Light | #52B788 | 渐变、标签、hover 态 |
| Primary Pale | #E8F5E9 | 标签背景、卡片 hover |
| Background | #F0F7F4 | 页面底色 |
| Surface | #FFFFFF | 卡片、容器 |
| Text Primary | #1A1A2E | 正文 |
| Text Secondary | #6B7280 | 辅助信息 |
| Accent Coral | #FF6B6B | 点赞、收藏、通知 |

### Typography
- 中文字体：PingFang SC / Microsoft YaHei
- 标题：1.5rem ~ 2.25rem, font-weight: 700
- 正文：0.875rem ~ 1rem, line-height: 1.75
- 辅助：0.75rem, color: #6B7280

### Border Radius
- 卡片：12px
- 按钮：8px
- 标签：20px (pill)
- 输入框：8px

## Directory Structure

```
blog-frontend/
├── pages/
│   ├── index.vue                 # 首页（混合布局）
│   ├── article/
│   │   ├── [id].vue              # 文章详情
│   │   ├── write.vue             # 发帖/编辑
│   │   └── hot.vue               # 热门排行
│   ├── tag/
│   │   ├── index.vue             # 标签云总览
│   │   └── [id].vue              # 标签筛选文章列表
│   ├── user/
│   │   ├── login.vue             # 登录
│   │   ├── register.vue          # 注册
│   │   └── [id].vue              # 个人主页
│   └── admin/
│       ├── tags.vue              # 标签管理
│       └── comments.vue          # 评论管理
├── components/
│   ├── layout/
│   │   ├── AppHeader.vue         # 全局头部导航
│   │   ├── AppFooter.vue         # 全局页脚
│   │   └── AppSidebar.vue        # 侧边栏（可选）
│   ├── article/
│   │   ├── ArticleCard.vue       # 瀑布流卡片
│   │   ├── ArticleList.vue       # 文章列表（含分页/无限滚动）
│   │   ├── ArticleEditor.vue     # 文章编辑器（Markdown）
│   │   └── ArticleStats.vue      # 文章统计栏
│   ├── comment/
│   │   ├── CommentList.vue       # 评论列表（嵌套）
│   │   └── CommentItem.vue       # 单条评论
│   └── common/
│       ├── UserAvatar.vue        # 用户头像
│       ├── TagBadge.vue          # 标签徽章
│       └── EmptyState.vue        # 空状态
├── composables/
│   ├── useAuth.ts                # 认证相关逻辑
│   ├── useArticle.ts             # 文章 CRUD + 列表
│   ├── useComment.ts             # 评论相关
│   └── useToggle.ts              # 点赞/收藏通用 toggle
├── stores/
│   ├── auth.ts                   # 认证状态（token, user, isAdmin）
│   └── ui.ts                     # UI 状态（sidebar, scroll）
├── api/
│   ├── index.ts                  # $fetch 封装 + 拦截器
│   └── modules/
│       ├── auth.ts               # /api/auth/*
│       ├── article.ts            # /api/articles/*
│       ├── comment.ts            # /api/comments + /api/articles/:id/comments
│       └── tag.ts                # /api/tags/*
├── types/
│   └── index.ts                  # TypeScript 类型定义
├── assets/
│   └── css/
│       └── main.css              # 全局样式 + 主题 CSS 变量
├── middleware/
│   └── auth.ts                   # 路由鉴权中间件
├── nuxt.config.ts
├── tailwind.config.ts
└── package.json
```

## Route Design

| Route | Page | Render | Auth | Data |
|-------|------|--------|------|------|
| `/` | 首页 | SSG | Public | 文章列表分页、热门横滑、标签云 |
| `/article/[id]` | 文章详情 | SSR | Public | 文章内容、评论、标签、统计 |
| `/article/write` | 发帖/编辑 | SPA | Required | 编辑器、草稿恢复 |
| `/article/hot` | 热门排行 | ISR | Public | 热榜（7d/30d） |
| `/tag/index` | 标签云 | SSG | Public | 全部标签 |
| `/tag/[id]` | 标签筛选 | SSR | Public | 按标签文章列表 |
| `/user/[id]` | 个人主页 | SSR | Public | 用户文章列表 |
| `/user/login` | 登录 | SPA | Guest | - |
| `/user/register` | 注册 | SPA | Guest | - |
| `/admin/tags` | 标签管理 | SPA | Admin | 标签 CRUD |
| `/admin/comments` | 评论管理 | SPA | Admin | 评论隐藏/删除 |

**Render Strategy:**
- 公开内容页 → SSR/SSG，保证 SEO 和首屏速度
- 需登录页 → SPA (CSR-only)，通过 Pinia auth store + JWT 鉴权
- 热门排行 → ISR（定时增量生成）

## API Layer Design

### Request Flow

```
Page/Component → composable → api/modules/* → api/index.ts ($fetch)
                                                      │
                                              Interceptors:
                                              1. Attach Authorization header
                                              2. Check JWT expiry
                                              3. Unwrap Result<T>
                                              4. Handle errors
```

### $fetch Wrapper (`api/index.ts`)

- **Request interceptor:** Read token from Pinia auth store → set `Authorization: Bearer <token>`
- **Response interceptor:** 
  - `code === 200` → return `data`
  - `code === 401` → clear auth, redirect to login
  - Other errors → Naive UI `message.error()` + throw
- **Network errors:** Unified error toast + data for retry button

### API Modules

Each module exports typed functions matching backend endpoints:

- `auth.ts`: register, login, logout
- `article.ts`: create, list, getById, update, patch, delete, like, favorite, stats, getTags, setTags, getHot, getHistory, getHistoryDetail, rollback, categoryStatistics
- `comment.ts`: create, getTopLevel, getReplies, toggleLike, delete, hide
- `tag.ts`: create, update, delete, getCloud

### TypeScript Types

All backend entities and DTOs mapped to TS interfaces:
- `Article`, `User`, `Comment`, `Tag`, `ArticleTag`
- `Result<T>`, `ToggleResult`, `LoginResponse`, `CommentDTO`
- `ArticleQueryDTO`, `HotArticleDTO`, `TagCloudItem`

## State Management (Pinia)

### Auth Store

```ts
{
  token: string | null
  user: { userId, username, role } | null
  isLoggedIn: computed
  isAdmin: computed
  login(credentials) → Promise
  register(credentials) → Promise
  logout() → Promise
  restoreFromStorage() → void
}
```

### UI Store

```ts
{
  sidebarOpen: boolean
  scrollPosition: number
  toggleSidebar()
}
```

## Page States & Error Handling

Every async component covers four states:

| State | UI | Example |
|-------|-----|---------|
| **Loading** | Naive Skeleton / Spin | 文章卡片骨架屏，详情页 Spin |
| **Empty** | Naive Empty + CTA | "还没有文章，成为第一个创作者吧～" |
| **Error** | Naive Result + Retry | 网络错误 + "重新加载"按钮 |
| **Success** | Data rendering | 正常展示 |

### Per-Page Special Handling

| Page | Extra States |
|------|-------------|
| 首页瀑布流 | 无限滚动 Load More / "没有更多了" |
| 文章详情 `/article/[id]` | 404 → "文章不存在", 已删除提示 |
| 发帖 `/article/write` | 草稿 localStorage 自动保存, 敏感词拒绝 |
| 评论 | 发布 loading, 敏感词拒绝提示 |
| 点赞/收藏 | 乐观更新 + 失败静默回滚 |
| 登录/注册 | 表单校验, 用户名冲突, 密码错误 |
| 管理页 | 403 无权限拦截 |

### Global Error Boundary
- `layouts/error.vue` — Nuxt 全局错误页
- Naive UI `useMessage` + `useDialog` — 操作级反馈
- 未捕获错误 → "出错了" + 返回首页

## Implementation Phases

### P1: Foundation + Core Browsing
- Project scaffolding (Nuxt 3 + Naive UI + Tailwind + Pinia)
- Theme system (Forest Green CSS variables)
- Layout components (Header, Footer)
- API wrapper + interceptors
- Auth (login, register, JWT management)
- Home page (Banner + hot scroll + waterfall cards)
- Article detail page (content, stats)

### P2: Content Creation + Interaction
- Article editor (write/edit, draft auto-save)
- Comment system (list, create, nested replies)
- Like/favorite toggle (optimistic update)
- Personal profile page

### P3: Discovery
- Tag cloud page + tag-filtered listing
- Hot articles page
- Search/filter on home page
- Category statistics

### P4: Advanced Features
- Article version history viewer
- Rollback UI
- Admin tag management
- Admin comment management
- Polish & animation

## Backend API Reference

Full 28 endpoints documented in `blog-backend/CLAUDE.md`. All responses wrapped in `Result<T> { code, message, data }`. Auth via `Authorization: Bearer <JWT>`. Public routes: `/api/auth/**`. All others require valid JWT.
