# 需求五：评论系统 — 设计文档

> 2026-06-04 · 状态：已批准

## 概述

为博客系统添加嵌套评论功能，支持多级回复、评论点赞、敏感词过滤、删除策略。

## 数据表

```sql
CREATE TABLE IF NOT EXISTS comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id  BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    parent_id   BIGINT,
    reply_to    BIGINT,
    content     TEXT         NOT NULL,
    like_count  INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'visible',
    created_at  DATETIME     NOT NULL,
    INDEX idx_article_parent (article_id, parent_id),
    INDEX idx_parent (parent_id)
);

CREATE TABLE IF NOT EXISTS comment_like (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    created_at  DATETIME NOT NULL,
    UNIQUE KEY uk_comment_user (comment_id, user_id)
);
```

- `parent_id`：父评论 ID，顶级为 NULL
- `reply_to`：被回复的评论 ID
- `status`：visible / hidden / deleted

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/comments` | 发表评论 |
| GET | `/api/articles/{id}/comments?page=&size=&sort=time\|like` | 顶级评论 + 每条带 3 条子回复 |
| GET | `/api/comments/{id}/replies?page=&size=` | 更多子回复 |
| POST | `/api/comments/{id}/like` | 点赞 toggle |
| DELETE | `/api/comments/{id}` | 删除（本人软删/管理员硬删） |
| PUT | `/api/comments/{id}/hide` | 管理员隐藏 |

## 核心逻辑

### 敏感词
- 敏感词列表配置在 application.yml：`app.sensitive-words`
- 发布时检查 content 是否包含任一敏感词，命中则返回 400

### 子回复加载
- 查顶级评论时，每条通过子查询带回最新 3 条子回复（status='visible'）
- 更多子回复通过 GET /api/comments/{id}/replies 懒加载

### 删除
- 评论作者：status → 'deleted'，内容保留，子回复不变
- 管理员：递归硬删除该评论及所有子孙
- 管理员标记：status → 'hidden'，不对外展示

### 计数
- article 表维护 comment_count，新增/删除时原子更新

### 缓存
- 顶级评论第一页缓存 5 分钟
- 新增/删除评论时失效

## 文件变更

| 操作 | 文件 |
|------|------|
| 新增 | entity/Comment.java, entity/CommentLike.java |
| 新增 | mapper/CommentMapper.java, mapper/CommentLikeMapper.java |
| 新增 | service/CommentService.java, service/impl/CommentServiceImpl.java |
| 新增 | controller/CommentController.java |
| 新增 | dto/CommentDTO.java（含子回复列表） |
| 修改 | schema.sql, Article.java, ArticleMapper.java, ArticleServiceImpl.java, application.yml, CLAUDE.md |
