# 文章版本历史与回滚 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add version history and rollback for published articles, plus draft/published state machine for the article listing API.

**Architecture:** New `article_history` table stores immutable snapshots before each edit. History is saved in `ArticleServiceImpl.updateById()` and `patch()` when content actually changes and article is PUBLISHED. Rollback copies old state to article table and records a new history entry with `change_type=ROLLBACK`. Article list defaults to `status=published`.

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL

---

## File Map

| Action | File | Purpose |
|--------|------|---------|
| Modify | `src/main/resources/schema.sql` | Add article_history table |
| Create | `src/main/java/com/blog/entity/ArticleHistory.java` | Entity |
| Modify | `src/main/java/com/blog/entity/Article.java` | Add PUBLISHED/DRAFT constants |
| Create | `src/main/java/com/blog/mapper/ArticleHistoryMapper.java` | Mapper |
| Create | `src/main/java/com/blog/service/ArticleHistoryService.java` | Interface |
| Create | `src/main/java/com/blog/service/impl/ArticleHistoryServiceImpl.java` | Implementation |
| Modify | `src/main/java/com/blog/service/impl/ArticleServiceImpl.java` | saveHistoryIfNeeded + cleanup + page filter + updateById/patch changes |
| Modify | `src/main/java/com/blog/controller/ArticleController.java` | 3 new endpoints |
| Create | `src/main/java/com/blog/task/HistoryCleanupScheduler.java` | Scheduled cleanup |
| Modify | `CLAUDE.md` | Update docs |

---

### Task 1: Add article_history table

**Files:**
- Modify: `src/main/resources/schema.sql` (append after article_tag)

- [ ] **Step 1: Add DDL**

Append after the last `CREATE TABLE`:

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

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/schema.sql
git commit -m "feat: add article_history table"
```

---

### Task 2: Create ArticleHistory entity

**Files:**
- Create: `src/main/java/com/blog/entity/ArticleHistory.java`

- [ ] **Step 1: Create entity**

```java
package com.blog.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_history")
public class ArticleHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private String title;

    private String content;

    private String category;

    private Integer versionNo;

    private String changeType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/blog/entity/ArticleHistory.java
git commit -m "feat: add ArticleHistory entity"
```

---

### Task 3: Add status constants to Article

**Files:**
- Modify: `src/main/java/com/blog/entity/Article.java`

- [ ] **Step 1: Add constants**

Add inside the class body, after the existing fields:

```java
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PUBLISHED = "published";
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/blog/entity/Article.java
git commit -m "feat: add Article status constants"
```

---

### Task 4: Create ArticleHistoryMapper

**Files:**
- Create: `src/main/java/com/blog/mapper/ArticleHistoryMapper.java`

- [ ] **Step 1: Create mapper**

```java
package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleHistoryMapper extends BaseMapper<ArticleHistory> {

    @Select("SELECT * FROM article_history WHERE article_id = #{articleId} ORDER BY version_no DESC")
    List<ArticleHistory> selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT COALESCE(MAX(version_no), 0) FROM article_history WHERE article_id = #{articleId}")
    int getMaxVersionNo(@Param("articleId") Long articleId);

    @Delete("DELETE FROM article_history WHERE article_id = #{articleId} " +
            "AND id NOT IN (SELECT * FROM (" +
            "  SELECT id FROM article_history WHERE article_id = #{articleId} " +
            "  ORDER BY version_no DESC LIMIT #{keepCount}" +
            ") t)")
    int deleteOldVersions(@Param("articleId") Long articleId, @Param("keepCount") int keepCount);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/blog/mapper/ArticleHistoryMapper.java
git commit -m "feat: add ArticleHistoryMapper"
```

---

### Task 5: Create ArticleHistoryService interface

**Files:**
- Create: `src/main/java/com/blog/service/ArticleHistoryService.java`

- [ ] **Step 1: Create interface**

```java
package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.entity.Article;
import com.blog.entity.ArticleHistory;

public interface ArticleHistoryService {

    IPage<ArticleHistory> getHistory(Long articleId, int page, int size);

    ArticleHistory getDetail(Long historyId);

    Article rollback(Long articleId, Long historyId, Long userId);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/blog/service/ArticleHistoryService.java
git commit -m "feat: add ArticleHistoryService interface"
```

---

### Task 6: Create ArticleHistoryServiceImpl

**Files:**
- Create: `src/main/java/com/blog/service/impl/ArticleHistoryServiceImpl.java`

- [ ] **Step 1: Create implementation**

```java
package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Article;
import com.blog.entity.ArticleHistory;
import com.blog.mapper.ArticleHistoryMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleHistoryServiceImpl implements ArticleHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ArticleHistoryServiceImpl.class);
    private static final int MAX_VERSIONS = 20;

    @Autowired
    private ArticleHistoryMapper articleHistoryMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public IPage<ArticleHistory> getHistory(Long articleId, int page, int size) {
        List<ArticleHistory> all = articleHistoryMapper.selectByArticleId(articleId);
        int total = all.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        Page<ArticleHistory> result = new Page<>(page, size, total);
        if (fromIndex < total) {
            result.setRecords(all.subList(fromIndex, toIndex));
        } else {
            result.setRecords(List.of());
        }
        return result;
    }

    @Override
    public ArticleHistory getDetail(Long historyId) {
        ArticleHistory history = articleHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new RuntimeException("History not found: " + historyId);
        }
        return history;
    }

    @Override
    @Transactional
    public Article rollback(Long articleId, Long historyId, Long userId) {
        if (userId == null || userId != 1) {
            throw new RuntimeException("Admin access required");
        }

        articleHistoryMapper.selectById(historyId);
        ArticleHistory target = getDetail(historyId);
        if (!target.getArticleId().equals(articleId)) {
            throw new RuntimeException("History does not belong to this article");
        }

        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("Article not found: " + articleId);
        }

        // Save current state as history before rollback
        int nextVersion = articleHistoryMapper.getMaxVersionNo(articleId) + 1;
        ArticleHistory preRollback = new ArticleHistory();
        preRollback.setArticleId(articleId);
        preRollback.setTitle(article.getTitle());
        preRollback.setContent(article.getContent());
        preRollback.setCategory(article.getCategory());
        preRollback.setVersionNo(nextVersion);
        preRollback.setChangeType("ROLLBACK");
        articleHistoryMapper.insert(preRollback);

        // Overwrite article with target history content
        article.setTitle(target.getTitle());
        article.setContent(target.getContent());
        article.setCategory(target.getCategory());
        articleMapper.updateById(article);

        // Cleanup old versions
        articleHistoryMapper.deleteOldVersions(articleId, MAX_VERSIONS);

        // Invalidate cache
        deleteCache("article::" + articleId);

        log.info("Rollback: articleId={} to historyId={} (version {})", articleId, historyId, target.getVersionNo());
        return article;
    }

    private void deleteCache(String key) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete cache: key={}", key, e);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/blog/service/impl/ArticleHistoryServiceImpl.java
git commit -m "feat: add ArticleHistoryServiceImpl with rollback logic"
```

---

### Task 7: Modify ArticleServiceImpl — history saving + page filter

**Files:**
- Modify: `src/main/java/com/blog/service/impl/ArticleServiceImpl.java`

- [ ] **Step 1: Add imports**

Add to import block:
```java
import com.blog.entity.ArticleHistory;
import com.blog.mapper.ArticleHistoryMapper;
import java.util.Objects;
```

- [ ] **Step 2: Inject ArticleHistoryMapper**

Add before existing `@Autowired` fields:
```java
    @Autowired
    private ArticleHistoryMapper articleHistoryMapper;
```

- [ ] **Step 3: Add history constants**

Add after `private static final Duration CACHE_TTL`:
```java
    private static final int MAX_HISTORY_VERSIONS = 20;
```

- [ ] **Step 4: Modify updateById**

Replace the existing `updateById` method:

```java
    @Override
    @Transactional
    public boolean updateById(Article entity) {
        Article old = getById(entity.getId());
        saveHistoryIfNeeded(old, entity);
        boolean result = super.updateById(entity);
        deleteCache(CACHE_ARTICLE + entity.getId());
        deleteCache(CACHE_CATEGORY_STATS);
        deleteTagCloudCache();
        sendEvent("updated", entity);
        return result;
    }
```

- [ ] **Step 5: Modify patch**

Replace the existing `patch` method:

```java
    @Override
    @Transactional
    public Article patch(Long id, Article partial) {
        Article old = getById(id);

        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Article::getId, id);

        boolean hasUpdate = false;
        if (partial.getTitle() != null) {
            wrapper.set(Article::getTitle, partial.getTitle());
            hasUpdate = true;
        }
        if (partial.getContent() != null) {
            wrapper.set(Article::getContent, partial.getContent());
            hasUpdate = true;
        }
        if (partial.getCategory() != null) {
            wrapper.set(Article::getCategory, partial.getCategory());
            hasUpdate = true;
        }
        if (partial.getStatus() != null) {
            wrapper.set(Article::getStatus, partial.getStatus());
            hasUpdate = true;
        }

        if (hasUpdate) {
            // Build the projected new article for comparison
            Article projected = new Article();
            projected.setId(id);
            projected.setTitle(partial.getTitle() != null ? partial.getTitle() : old.getTitle());
            projected.setContent(partial.getContent() != null ? partial.getContent() : old.getContent());
            projected.setCategory(partial.getCategory() != null ? partial.getCategory() : old.getCategory());
            projected.setStatus(partial.getStatus() != null ? partial.getStatus() : old.getStatus());
            saveHistoryIfNeeded(old, projected);

            wrapper.set(Article::getUpdatedAt, LocalDateTime.now());
            baseMapper.update(wrapper);
        }

        Article updated = super.getById(id);
        deleteCache(CACHE_ARTICLE + id);
        deleteCache(CACHE_CATEGORY_STATS);
        deleteTagCloudCache();
        sendEvent("updated", updated);
        return updated;
    }
```

- [ ] **Step 6: Add saveHistoryIfNeeded method**

Add before the cache helper methods:

```java
    // ==================== 版本历史 ====================

    private void saveHistoryIfNeeded(Article oldArticle, Article newArticle) {
        if (!Article.STATUS_PUBLISHED.equals(oldArticle.getStatus())) {
            return;
        }

        boolean contentChanged =
                !Objects.equals(oldArticle.getTitle(), newArticle.getTitle()) ||
                !Objects.equals(oldArticle.getContent(), newArticle.getContent()) ||
                !Objects.equals(oldArticle.getCategory(), newArticle.getCategory());

        if (!contentChanged) {
            return;
        }

        int nextVersion = articleHistoryMapper.getMaxVersionNo(oldArticle.getId()) + 1;

        ArticleHistory history = new ArticleHistory();
        history.setArticleId(oldArticle.getId());
        history.setTitle(oldArticle.getTitle());
        history.setContent(oldArticle.getContent());
        history.setCategory(oldArticle.getCategory());
        history.setVersionNo(nextVersion);
        history.setChangeType("UPDATE");
        articleHistoryMapper.insert(history);

        articleHistoryMapper.deleteOldVersions(oldArticle.getId(), MAX_HISTORY_VERSIONS);
    }
```

- [ ] **Step 7: Modify page() — default to published**

Replace the status filtering block in `page()`:

Old:
```java
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(Article::getStatus, query.getStatus());
        }
```

New:
```java
        String status = query.getStatus();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Article::getStatus, status);
        } else {
            wrapper.eq(Article::getStatus, Article.STATUS_PUBLISHED);
        }
```

Also update the `selectByTagId` call in the tagId branch to pass the default status:

Replace:
```java
            return baseMapper.selectByTagId(page,
                    query.getTagId(),
                    query.getCategory(),
                    query.getStatus(),
                    query.getKeyword());
```

With:
```java
            String status = query.getStatus() != null && !query.getStatus().isBlank()
                    ? query.getStatus() : Article.STATUS_PUBLISHED;
            return baseMapper.selectByTagId(page,
                    query.getTagId(),
                    query.getCategory(),
                    status,
                    query.getKeyword());
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/blog/service/impl/ArticleServiceImpl.java
git commit -m "feat: add history saving on article update, default list to published"
```

---

### Task 8: Modify ArticleController — add history/rollback endpoints

**Files:**
- Modify: `src/main/java/com/blog/controller/ArticleController.java`

- [ ] **Step 1: Add imports**

Add:
```java
import com.blog.entity.ArticleHistory;
import com.blog.service.ArticleHistoryService;
```

- [ ] **Step 2: Inject ArticleHistoryService**

Add to constructor fields:
```java
    private final ArticleHistoryService articleHistoryService;
```

- [ ] **Step 3: Add three endpoints**

Insert before the `@GetMapping("/hot")` endpoint:

```java
    @GetMapping("/{id}/history")
    public Result<IPage<ArticleHistory>> history(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        return Result.ok(articleHistoryService.getHistory(id, page, size));
    }

    @GetMapping("/{id}/history/{historyId}")
    public Result<ArticleHistory> historyDetail(@PathVariable Long id,
                                                 @PathVariable Long historyId) {
        ArticleHistory history = articleHistoryService.getDetail(historyId);
        if (!history.getArticleId().equals(id)) {
            return Result.fail(400, "History does not belong to this article");
        }
        return Result.ok(history);
    }

    @PostMapping("/{id}/rollback/{historyId}")
    public Result<Article> rollback(@PathVariable Long id,
                                     @PathVariable Long historyId,
                                     @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.ok(articleHistoryService.rollback(id, historyId, userId));
    }
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/blog/controller/ArticleController.java
git commit -m "feat: add history list, detail, and rollback endpoints"
```

---

### Task 9: Create HistoryCleanupScheduler

**Files:**
- Create: `src/main/java/com/blog/task/HistoryCleanupScheduler.java`

- [ ] **Step 1: Create scheduler**

```java
package com.blog.task;

import com.blog.mapper.ArticleHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HistoryCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(HistoryCleanupScheduler.class);
    private static final int MAX_VERSIONS = 20;

    @Autowired
    private ArticleHistoryMapper articleHistoryMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOrphanHistories() {
        log.info("Running history cleanup...");
        List<Long> articleIds = articleHistoryMapper.selectList(null)
                .stream()
                .map(h -> h.getArticleId())
                .distinct()
                .toList();

        for (Long articleId : articleIds) {
            try {
                articleHistoryMapper.deleteOldVersions(articleId, MAX_VERSIONS);
            } catch (Exception e) {
                log.warn("Failed to cleanup history for articleId={}", articleId, e);
            }
        }
        log.info("History cleanup complete, processed {} articles", articleIds.size());
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/blog/task/HistoryCleanupScheduler.java
git commit -m "feat: add HistoryCleanupScheduler for nightly cleanup"
```

---

### Task 10: Update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update entity count (6 → 7)**

Replace:
```
├── entity/          # 6 个实体：Article, ArticleLike, ArticleFavorite, ArticleRead, Tag, ArticleTag
├── mapper/          # 6 个 Mapper，继承 BaseMapper，自定义 SQL 用注解
├── service/         # 6 个 Service 接口
├── service/impl/    # 6 个 Service 实现
```
With:
```
├── entity/          # 7 个实体：Article, ArticleLike, ArticleFavorite, ArticleRead, Tag, ArticleTag, ArticleHistory
├── mapper/          # 7 个 Mapper
├── service/         # 7 个 Service 接口
├── service/impl/    # 7 个 Service 实现
```

- [ ] **Step 2: Update tables (6 → 7)**

Replace "6 张表" with "7 张表". Add to table list:
```
| `article_history` | 文章版本历史 | INDEX(article_id, version_no), change_type |
```

- [ ] **Step 3: Add new endpoints to API table**

After the rollback endpoint row, add:
```
| GET | `/api/articles/{id}/history?page=&size=` | 版本历史列表 |
| GET | `/api/articles/{id}/history/{historyId}` | 版本详情 |
| POST | `/api/articles/{id}/rollback/{historyId}` | 回滚到指定版本（管理员） |
```

- [ ] **Step 4: Add version history section**

Before "## 本地开发环境切换", add:

```markdown
## 文章版本历史

### 触发条件
- 文章状态为 `published` 且 title/content/category 任一变化时，自动保存一次历史快照
- 草稿（draft）状态不生成历史
- PUT 和 PATCH 均触发同一检查逻辑

### 版本策略
- 每篇文章独立递增版本号（version_no）
- 最多保留 20 个版本，超过后自动删除最旧版本
- `change_type`: `UPDATE`（普通编辑）或 `ROLLBACK`（回滚操作）

### 回滚
- 将当前文章内容覆盖为目标版本内容
- 回滚前自动保存当前状态为一条新历史（change_type=ROLLBACK）
- 需管理员权限（X-User-Id: 1）
```

- [ ] **Step 5: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md with version history system"
```

---

### Task 11: Compile and verify

**Files:** None (verification only)

- [ ] **Step 1: Compile**

```bash
cd D:/vibecoding1/blog-backend && mvn compile -f pom.xml
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Start and test**

```bash
# Start app
mvn spring-boot:run -f pom.xml

# Test: create and publish article
curl -s -X POST http://localhost:8080/api/articles \
  -H "Content-Type: application/json" \
  -d '{"title":"v1","content":"first version","category":"test","status":"draft"}'

# Publish it
curl -s -X PATCH http://localhost:8080/api/articles/NEW_ID \
  -H "Content-Type: application/json" \
  -d '{"status":"published"}'

# Edit content (should create history)
curl -s -X PUT http://localhost:8080/api/articles/NEW_ID \
  -H "Content-Type: application/json" \
  -d '{"title":"v2","content":"second version","category":"test","status":"published"}'

# Check history
curl -s http://localhost:8080/api/articles/NEW_ID/history

# Rollback to first version
curl -s -X POST http://localhost:8080/api/articles/NEW_ID/rollback/FIRST_HISTORY_ID \
  -H "X-User-Id: 1"

# Verify article content was restored
curl -s http://localhost:8080/api/articles/NEW_ID
```
