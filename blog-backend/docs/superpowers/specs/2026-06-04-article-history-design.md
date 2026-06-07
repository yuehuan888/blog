# 需求四：文章版本历史与回滚 — 设计文档

> 2026-06-04 · 状态：已批准

## 概述

为文章系统添加版本历史与回滚功能，同时引入草稿/发布状态机。

## 状态机

```
DRAFT ──→ PUBLISHED
```

- `DRAFT`：草稿，可反复保存，不对外显示，不生成历史版本
- `PUBLISHED`：已发布，对外可见，编辑内容时生成历史版本
- `ARCHIVED` 延期

## 数据库

```sql
CREATE TABLE IF NOT EXISTS article_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id  BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    category    VARCHAR(100),
    version_no  INT          NOT NULL,
    change_type VARCHAR(20)  NOT NULL DEFAULT 'UPDATE',
    created_at  DATETIME     NOT NULL,
    INDEX idx_article_version (article_id, version_no)
);
```

- `change_type`: `UPDATE` | `ROLLBACK`
- `version_no` 按文章独立递增，不因清理重置
- 只快照 title/content/category

## 核心逻辑

### 历史保存触发条件（封装在 ArticleServiceImpl 私有方法中）

```
article.status == PUBLISHED
  AND (title 有变化 OR content 有变化 OR category 有变化)
  → 插入 article_history → 清理超出 20 个的旧版本
```

- PUT 和 PATCH 都触发同一检查
- 仅改 status 不记历史
- 草稿状态任何修改不记历史

### 版本数量控制

- 每篇最多 20 个版本
- 插入后 `DELETE FROM article_history WHERE article_id=? AND id NOT IN (SELECT id FROM (SELECT id FROM article_history WHERE article_id=? ORDER BY version_no DESC LIMIT 20) t)`
- 版本号永远递增

### 回滚

1. 读取 history 记录
2. 覆盖 article 主表 (title, content, category)
3. 插入新 history：change_type=ROLLBACK, version_no+1
4. 清理超出 20 个版本
5. 失效 article 缓存

### 文章列表

- `GET /api/articles` 默认只返回 `status='published'`
- `GET /api/articles?status=draft` 可查草稿（作者视角）

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/articles/{id}/history?page=&size=` | 版本列表（version_no 降序） |
| GET | `/api/articles/{id}/history/{historyId}` | 版本详情 |
| POST | `/api/articles/{id}/rollback/{historyId}` | 回滚（管理员） |

## 文件变更

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `entity/ArticleHistory.java` | |
| 新增 | `mapper/ArticleHistoryMapper.java` | 含 deleteOldVersions 方法 |
| 新增 | `service/ArticleHistoryService.java` | |
| 新增 | `service/impl/ArticleHistoryServiceImpl.java` | |
| 新增 | `task/HistoryCleanupScheduler.java` | 定时兜底清理 |
| 修改 | `schema.sql` | 加 article_history 表 |
| 修改 | `entity/Article.java` | status 常量 |
| 修改 | `ArticleServiceImpl.java` | updateById/patch 触发历史 + saveHistory/cleanupOldVersions 私有方法 |
| 修改 | `ArticleController.java` | history/rollback 端点 + 列表默认过滤 |
| 修改 | `CLAUDE.md` | 更新文档 |
