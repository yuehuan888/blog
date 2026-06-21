# 文章图片上传与展示 — 设计规范

> 日期：2026-06-21 | 状态：设计中

## 概述

为 GreenRead 博客平台添加文章配图功能。用户写文章时可上传多张图片，首页卡片以瀑布流展示封面图，详情页以横向轮播展示全部图片。设计对标小红书图文笔记体验。

## 数据库变更

### 新增表：article_image

```sql
CREATE TABLE IF NOT EXISTS article_image (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_article_id (article_id)
);
```

| 字段 | 说明 |
|------|------|
| `article_id` | 关联文章 ID，级联删除 |
| `url` | 图片相对路径，如 `/uploads/articles/article_abc123.png` |
| `sort_order` | 排序序号，0 为封面图 |

## 后端变更

### 新增组件

| 组件 | 说明 |
|------|------|
| `ArticleImage.java` | 实体，映射 `article_image` 表 |
| `ArticleImageMapper.java` | 继承 BaseMapper，自定义 `deleteByArticleId`、`selectByArticleId` |
| `POST /api/upload/article-image` | 图片上传端点 |

### 上传端点

- **路径**：`POST /api/upload/article-image`
- **鉴权**：需登录（PUT/POST 默认拦截）
- **参数**：`file`（MultipartFile），可选 `sort_order`
- **校验**：文件非空、MIME 为 `image/*`、单文件 ≤ 5MB
- **存储**：`{app.upload.dir}/articles/article_{uuid8}{ext}`
- **返回**：`{ "url": "/uploads/articles/article_xxx.png" }`

### 文章创建/更新

- `createArticle` / `updateArticle` 请求体新增可选字段 `imageUrls: string[]`
- 后端按数组顺序写入 `article_image`（`sort_order` = 数组下标）
- 更新时：先删除旧记录和文件，再写入新记录

### 文章查询

- **列表接口**（`GET /api/articles`）：Article 返回新增 `coverImage` 字段（`sort_order=0` 的 URL，无图时 null）
- **详情接口**（`GET /api/articles/{id}`）：Article 返回新增 `images: string[]` 字段（按 `sort_order` 排序的全部 URL）

### 级联删除

`ArticleServiceImpl.removeById()` 新增步骤：
1. 查询 `article_image` 列表
2. 删除服务器上的图片文件
3. 删除 `article_image` 表记录
4. 删除 Redis 文章缓存

### 孤儿文件清理

定时任务 `ImageCleanupScheduler`：每天凌晨 3 点扫描 `uploads/articles/` 目录，删除 `article_image` 表中无对应记录且创建超过 24 小时的文件。

## 前端变更

### 首页瀑布流布局（index.vue）

**从固定 CSS Grid 改为 JS Masonry 瀑布流：**

- 用 JS 维护每列当前累积高度
- 每张卡片放入当前最矮列
- 卡片宽度由列数决定（响应式：2/3/4 列），高度由封面图比例自适应
- 无限滚动追加时继续放入最短列
- 切换标签时重置列高度状态

**列数响应式断点：**

| 断点 | 列数 |
|------|------|
| < 640px | 2 |
| 640–1024px | 3 |
| ≥ 1024px | 4 |

### 文章卡片改造（ArticleCard.vue）

**布局变更：**

```
┌──────────────┐
│              │
│   封面图片    │  ← 按原比例显示，object-fit: cover
│              │     无图时隐藏此区域
│  ┌──┐        │
│  │+3│ 角标   │  ← 图片数 > 1 时显示（右下角）
│  └──┘        │
├──────────────┤
│  文章标题     │  ← 最多 2 行，line-clamp-2
│  标签 (可选)  │
│  作者头像+名  │
│  👁 1.2k ❤ 89 💬 12 │
└──────────────┘
```

**图片区域：**
- 有图：显示封面图，按图片原始宽高比设置高度（最小 120px，最大 320px）
- 无图：回退为紧凑文字卡片（纯色背景 + emoji，维持当前风格但更矮）
- 图片加载中：骨架屏占位

### 详情页图片轮播（article/[id].vue）

**位置**：文章标题下方、标签/正文上方

```
[← 返回]
[作者信息 + 关注按钮]
[文章标题]
[分类 · 日期 · 👁 阅读]
────────────────────
│                    │
│  图片轮播区域       │  ← 360px 高度
│  ◄ 滑动 ►         │
│  ● ○ ○ ○  指示器  │
│                    │
────────────────────
[标签]
[点赞/收藏 状态栏]
[正文内容]
[评论区]
```

**轮播行为：**

| 场景 | 行为 |
|------|------|
| 多图 | 横向滑动 + 左右箭头 + 底部圆点指示器 |
| 单图 | 仅显示图片，无箭头和指示器 |
| 无图 | 整个区域不渲染 |
| 点击图片 | 弹出全屏预览遮层，支持双指缩放 |

**图片渲染**：`object-fit: contain`，容器背景色 `#f5f5f5` 消除黑边感。

### 写文章页上传区（article/write.vue）

**新布局顺序**：标题 → **图片上传区（新增）** → 标签选择 → 正文

```
┌─────────────────────────────────────┐
│ 标题：___________________________   │
├─────────────────────────────────────┤
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐       │
│ │    │ │    │ │    │ │    │       │
│ │ 图1│ │ 图2│ │ 图3│ │ 图4│       │  ← 缩略图网格（4列）
│ │ ✕  │ │ ✕  │ │ ✕  │ │ ✕  │       │
│ └────┘ └────┘ └────┘ └────┘       │
│ ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐     │
│ │       📷 添加图片         │     │  ← 上传触发区（虚线边框）
│ │   点击或拖拽，最多9张      │     │
│ └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘     │
├─────────────────────────────────────┤
│ 分类标签：[选择标签 ▼]              │
├─────────────────────────────────────┤
│ 内容：___________________________   │
└─────────────────────────────────────┘
```

**交互细节：**

| 操作 | 行为 |
|------|------|
| 选择图片 | 点击虚线区域或拖拽文件，支持一次选多张 |
| 即时上传 | 选中后立即 POST 到 `/api/upload/article-image`，显示进度 |
| 拖拽排序 | 缩略图可拖拽调整顺序，排序即 `sort_order` |
| 删除 | 每张图右上角 ✕，直接删（前端暂不调后端清理，发布时以最终列表为准） |
| 数量上限 | 最多 9 张，超过时 toast 提示 |
| 大小限制 | 单张 > 5MB 时拒绝，toast 提示 |

**草稿与图片：**
- 草稿数据（localStorage + 后端 draft）保存 `imageUrls` 数组
- 编辑草稿时恢复已上传图片的缩略图
- 已上传但最终未关联的图片由后端定时任务清理

### 类型定义变更（types/index.ts）

```typescript
// Article 接口新增
export interface Article {
  // ... 现有字段
  coverImage: string | null  // 列表用封面图
  images: string[]           // 详情用全部图片
}
```

### API 模块变更（api/modules/article.ts）

```typescript
// createArticle / updateArticle 参数新增
{ imageUrls?: string[] }
```

新增上传函数：
```typescript
export async function uploadArticleImage(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  const result = await apiRequest<{ url: string }>('/api/upload/article-image', {
    method: 'POST',
    body: formData,
  })
  return result.url
}
```

## 错误处理

| 场景 | 处理 |
|------|------|
| 图片上传失败 | toast 提示，图片槽位保留但标记失败，可重试 |
| 图片超尺寸 | 客户端预检 + 后端校验双重拦截 |
| 图片加载失败（卡片/轮播） | 显示占位图（灰色背景 + 图片破碎图标） |
| 文章保存时图片 URL 已失效 | 后端不做二次校验，前端正常展示 |

## 边界情况

| 场景 | 行为 |
|------|------|
| 无图文章 | 卡片回退为紧凑文字样式，详情页无轮播区域 |
| 仅 1 张图 | 卡片角标不显示，详情页轮播无控件 |
| 9 张图（上限） | 上传区隐藏 ➕ 按钮 |
| 编辑文章时删减图片 | 提交时传最终 imageUrls，后端覆写 article_image |
| 网络慢导致上传中 | 显示 NSpin 进度（非确定性） |

## 实现范围

### 本期实现
- article_image 表 + 完整 CRUD
- 图片上传端点
- 详情页轮播（纯 CSS scroll-snap + 手动实现）
- 写文章页上传区
- 首页瀑布流 + 卡片改造
- 级联删除 + 孤儿文件清理

### 后续优化
- 服务端图片压缩/缩略图
- 全屏预览手势（双指缩放）
- 图片 EXIF 方向自动纠正

## 不涉及的内容
- Markdown/富文本编辑器（仍用纯文本 textarea）
- 视频/GIF 支持
- 图片水印
