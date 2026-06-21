# 文章图片上传与展示 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 GreenRead 博客平台添加文章配图功能：用户写文章时可上传最多 9 张图片，首页瀑布流卡片展示封面图，详情页横向轮播展示全部图片，对标小红书图文笔记体验。

**Architecture:** 新增 `article_image` 表独立存储图片，后端扩展 ArticleDTO 返回 `coverImage`/`images`/`imageCount`，前端首页改为 JS Masonry 瀑布流、ArticleCard 改为封面图卡片、详情页加 CSS scroll-snap 轮播、写文章页加即时上传区。

**Tech Stack:** Spring Boot 3.2.5 + MyBatis-Plus 3.5.6 + Nuxt 3 + Naive UI + Tailwind CSS

## Global Constraints

- 图片单张 ≤ 5MB，仅限 `image/*` MIME
- 每篇文章最多 9 张图片
- 上传路径：`{app.upload.dir}/articles/`，URL 路径：`/uploads/articles/`
- JWT 鉴权：上传接口在 `/api/upload/**` 白名单中（写文章页需登录，但上传端点本身免鉴权以支持草稿场景）
- 首页瀑布流列数响应式：2 列(<640px) / 3 列(640-1024px) / 4 列(≥1024px)
- 卡片图片区域：最小 120px 高，最大 320px 高，按原图比例自适应
- 轮播容器高度 360px，`object-fit: contain`

---

### Task 1: 数据库 — 新增 article_image 表

**Files:**
- Modify: `blog-backend/src/main/resources/schema.sql` (append new table DDL)

- [ ] **Step 1: 在 schema.sql 末尾添加建表语句**

在 `schema.sql` 末尾追加：

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

- [ ] **Step 2: 手动执行 SQL 建表（已有数据库需要）**

```bash
# 连接到 MySQL 执行
mysql -u root -p123456 blogsystem -e "
CREATE TABLE IF NOT EXISTS article_image (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_article_id (article_id)
);
"
```

- [ ] **Step 3: 验证**

```bash
mysql -u root -p123456 blogsystem -e "DESC article_image;"
```

预期输出：显示 5 个字段（id, article_id, url, sort_order, created_at）。

- [ ] **Step 4: Commit**

```bash
git add blog-backend/src/main/resources/schema.sql
git commit -m "feat: add article_image table for article image attachments"
```

---

### Task 2: 后端 — ArticleImage 实体 + Mapper

**Files:**
- Create: `blog-backend/src/main/java/com/blog/entity/ArticleImage.java`
- Create: `blog-backend/src/main/java/com/blog/mapper/ArticleImageMapper.java`

**Interfaces:**
- Produces: `ArticleImage` entity (fields: id, articleId, url, sortOrder, createdAt)
- Produces: `ArticleImageMapper` with `deleteByArticleId(Long)` and `selectByArticleId(Long)`

- [ ] **Step 1: 创建 ArticleImage 实体**

`blog-backend/src/main/java/com/blog/entity/ArticleImage.java`：

```java
package com.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_image")
public class ArticleImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private String url;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: 创建 ArticleImageMapper**

`blog-backend/src/main/java/com/blog/mapper/ArticleImageMapper.java`：

```java
package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ArticleImage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ArticleImageMapper extends BaseMapper<ArticleImage> {

    @Delete("DELETE FROM article_image WHERE article_id = #{articleId}")
    int deleteByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT * FROM article_image WHERE article_id = #{articleId} ORDER BY sort_order")
    List<ArticleImage> selectByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT article_id, COUNT(*) AS cnt FROM article_image WHERE article_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY article_id")
    List<Map<String, Object>> countByArticleIds(@Param("ids") List<Long> articleIds);

    @Select("SELECT * FROM article_image WHERE article_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND sort_order = 0")
    List<ArticleImage> selectCoverImages(@Param("ids") List<Long> articleIds);
}
```

需要 import `java.util.Map`。

- [ ] **Step 3: 编译验证**

```bash
cd blog-backend && mvn compile -f pom.xml
```

预期：BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add blog-backend/src/main/java/com/blog/entity/ArticleImage.java blog-backend/src/main/java/com/blog/mapper/ArticleImageMapper.java
git commit -m "feat: add ArticleImage entity and mapper"
```

---

### Task 3: 后端 — 上传端点 /api/upload/article-image

**Files:**
- Modify: `blog-backend/src/main/java/com/blog/controller/UploadController.java` (add new method)

**Interfaces:**
- Consumes: `ArticleImageMapper` (Task 2 — but not needed for upload itself)
- Produces: `POST /api/upload/article-image` → `{ "url": "/uploads/articles/article_xxx.png" }`

- [ ] **Step 1: 在 UploadController 中添加新端点**

在 `UploadController.java` 中，在 `uploadAvatar` 方法之后添加：

```java
@PostMapping("/upload/article-image")
public Result<Map<String, String>> uploadArticleImage(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) {
        return Result.fail(400, "File is empty");
    }

    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
        return Result.fail(400, "Only image files are allowed");
    }

    if (file.getSize() > 5 * 1024 * 1024) {
        return Result.fail(400, "File size must be less than 5MB");
    }

    try {
        Path articleDir = Paths.get(uploadDir, "articles");
        Files.createDirectories(articleDir);

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String filename = "article_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        Path filePath = articleDir.resolve(filename);
        file.transferTo(filePath.toFile());

        String url = "/uploads/articles/" + filename;
        return Result.ok(Map.of("url", url));
    } catch (IOException e) {
        return Result.fail(500, "Failed to save file: " + e.getMessage());
    }
}
```

- [ ] **Step 2: 确认 AuthInterceptor 白名单已包含 /api/upload/**

检查 `WebMvcConfig.java:30` — 已有 `.excludePathPatterns("/api/auth/**", "/api/upload/**")`，新端点 `/api/upload/article-image` 自动匹配 `/api/upload/**`，无需修改。

- [ ] **Step 3: 编译验证**

```bash
cd blog-backend && mvn compile -f pom.xml
```

- [ ] **Step 4: Commit**

```bash
git add blog-backend/src/main/java/com/blog/controller/UploadController.java
git commit -m "feat: add /api/upload/article-image endpoint (5MB limit)"
```

---

### Task 4: 后端 — Article 实体扩展 + Service 层图片处理

**Files:**
- Modify: `blog-backend/src/main/java/com/blog/entity/Article.java` (add transient image fields)
- Modify: `blog-backend/src/main/java/com/blog/service/impl/ArticleServiceImpl.java` (inject ArticleImageMapper, image handling in CRUD)
- Modify: `blog-backend/src/main/java/com/blog/controller/ArticleController.java` (accept & return imageUrls)

**Interfaces:**
- Produces: Article 新增 transient 字段 `coverImage: String`、`images: List<String>`、`imageCount: Integer`
- Produces: `ArticleServiceImpl` 在 `getById` 中填充 images，在 `page` 中批量填充 coverImage/imageCount，在 `removeById` 中清理图片

- [ ] **Step 1: Article 实体添加 transient 字段**

在 `Article.java` 末尾（`updatedAt` 之后，类结束 `}` 之前）添加：

```java
@TableField(exist = false)
private String coverImage;

@TableField(exist = false)
private List<String> images;

@TableField(exist = false)
private Integer imageCount;
```

- [ ] **Step 2: ArticleServiceImpl 注入 ArticleImageMapper**

在 `ArticleServiceImpl.java` 中，在现有 `@Autowired` 区域（如 `tagMapper` 之后）添加：

```java
@Autowired
private ArticleImageMapper articleImageMapper;
```

添加 import：
```java
import com.blog.entity.ArticleImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.io.File;
```

- [ ] **Step 3: 修改 getById — 填充 images 和 coverImage**

在 `getById` 方法中，`setCache(key, article);` 之前添加：

```java
// Populate images
List<ArticleImage> articleImages = articleImageMapper.selectByArticleId((Long) id);
if (!articleImages.isEmpty()) {
    article.setImages(articleImages.stream()
            .map(ArticleImage::getUrl)
            .collect(Collectors.toList()));
    article.setCoverImage(articleImages.get(0).getUrl());
    article.setImageCount(articleImages.size());
} else {
    article.setImages(Collections.emptyList());
    article.setImageCount(0);
}
```

完整方法变为：

```java
@Override
public Article getById(Serializable id) {
    String key = CACHE_ARTICLE + id;
    Article cached = getCache(key, Article.class);
    if (cached != null) {
        return cached;
    }

    Article article = super.getById(id);
    if (article == null) {
        throw new RuntimeException("Article not found: " + id);
    }

    // Populate images
    List<ArticleImage> articleImages = articleImageMapper.selectByArticleId((Long) id);
    if (!articleImages.isEmpty()) {
        article.setImages(articleImages.stream()
                .map(ArticleImage::getUrl)
                .collect(Collectors.toList()));
        article.setCoverImage(articleImages.get(0).getUrl());
        article.setImageCount(articleImages.size());
    } else {
        article.setImages(Collections.emptyList());
        article.setImageCount(0);
    }

    setCache(key, article);
    return article;
}
```

- [ ] **Step 4: 修改 page — 批量填充 coverImage 和 imageCount**

在 `page(ArticleQueryDTO query)` 方法的 `return` 之前添加。找到方法的 return 语句（第 230 行 `return baseMapper.selectPage(page, wrapper);`），在两个 return 路径之前分别添加批量填充逻辑。

将两个 return 改为先赋值再统一处理。重写 `page` 方法尾部（从 `wrapper.orderByDesc` 开始）：

```java
wrapper.orderByDesc(Article::getCreatedAt);

Page<Article> page = new Page<>(query.getPage(), query.getSize());
IPage<Article> result = baseMapper.selectPage(page, wrapper);

// Batch load coverImage and imageCount
List<Article> records = result.getRecords();
if (!records.isEmpty()) {
    List<Long> articleIds = records.stream().map(Article::getId).collect(Collectors.toList());

    // Load cover images (sort_order = 0)
    List<ArticleImage> coverImages = articleImageMapper.selectCoverImages(articleIds);
    Map<Long, String> coverMap = new HashMap<>();
    for (ArticleImage img : coverImages) {
        coverMap.put(img.getArticleId(), img.getUrl());
    }

    // Load image counts
    List<Map<String, Object>> countRows = articleImageMapper.countByArticleIds(articleIds);
    Map<Long, Integer> countMap = new HashMap<>();
    for (Map<String, Object> row : countRows) {
        countMap.put(((Number) row.get("article_id")).longValue(),
                     ((Number) row.get("cnt")).intValue());
    }

    for (Article a : records) {
        a.setCoverImage(coverMap.get(a.getId()));
        a.setImageCount(countMap.getOrDefault(a.getId(), 0));
        a.setImages(Collections.emptyList()); // not loaded in list
    }
}

return result;
```

同时需要修改 tagId 筛选分支的 return（第 200 行附近）。将两个 return 路径统一：无论是否按 tagId 筛选，最后都用上述批量填充逻辑。tagId 分支改为：

```java
if (query.getTagId() != null) {
    Page<Article> page = new Page<>(query.getPage(), query.getSize());
    String tagStatus = query.getStatus() != null && !query.getStatus().isBlank()
            ? query.getStatus() : Article.STATUS_PUBLISHED;
    IPage<Article> result = baseMapper.selectByTagId(page,
            query.getTagId(),
            query.getCategory(),
            tagStatus,
            query.getKeyword(),
            query.getAuthorId());
    // Batch load coverImage and imageCount
    List<Article> records = result.getRecords();
    if (!records.isEmpty()) {
        List<Long> ids = records.stream().map(Article::getId).collect(Collectors.toList());
        Map<Long, String> coverMap = batchCoverMap(ids);
        Map<Long, Integer> countMap = batchCountMap(ids);
        for (Article a : records) {
            a.setCoverImage(coverMap.get(a.getId()));
            a.setImageCount(countMap.getOrDefault(a.getId(), 0));
            a.setImages(Collections.emptyList());
        }
    }
    return result;
}
```

提取两个私有辅助方法避免重复：

```java
private Map<Long, String> batchCoverMap(List<Long> articleIds) {
    List<ArticleImage> coverImages = articleImageMapper.selectCoverImages(articleIds);
    Map<Long, String> map = new HashMap<>();
    for (ArticleImage img : coverImages) {
        map.put(img.getArticleId(), img.getUrl());
    }
    return map;
}

private Map<Long, Integer> batchCountMap(List<Long> articleIds) {
    List<Map<String, Object>> rows = articleImageMapper.countByArticleIds(articleIds);
    Map<Long, Integer> map = new HashMap<>();
    for (Map<String, Object> row : rows) {
        map.put(((Number) row.get("article_id")).longValue(),
                ((Number) row.get("cnt")).intValue());
    }
    return map;
}
```

- [ ] **Step 5: 修改 save — 处理 imageUrls**

在 `save(Article entity)` 方法中，`boolean result = super.save(entity);` 之后、`deleteCache` 之前，添加：

```java
// Save images if provided
if (entity.getImages() != null && !entity.getImages().isEmpty()) {
    for (int i = 0; i < entity.getImages().size(); i++) {
        ArticleImage img = new ArticleImage();
        img.setArticleId(entity.getId());
        img.setUrl(entity.getImages().get(i));
        img.setSortOrder(i);
        articleImageMapper.insert(img);
    }
}
```

- [ ] **Step 6: 修改 updateById — 处理 imageUrls 更新**

在 `updateById(Article entity)` 方法中，`saveHistoryIfNeeded` 之前，添加图片更新逻辑：

```java
// Update images if provided
if (entity.getImages() != null) {
    // Delete old images and files
    List<ArticleImage> oldImages = articleImageMapper.selectByArticleId(entity.getId());
    for (ArticleImage oldImg : oldImages) {
        try {
            File file = new File(uploadDir, oldImg.getUrl().replace("/uploads/", ""));
            if (file.exists()) file.delete();
        } catch (Exception e) {
            log.warn("Failed to delete image file: {}", oldImg.getUrl(), e);
        }
    }
    articleImageMapper.deleteByArticleId(entity.getId());

    // Insert new images
    for (int i = 0; i < entity.getImages().size(); i++) {
        ArticleImage img = new ArticleImage();
        img.setArticleId(entity.getId());
        img.setUrl(entity.getImages().get(i));
        img.setSortOrder(i);
        articleImageMapper.insert(img);
    }
}
```

需要在 `ArticleServiceImpl` 中注入 `@Value("${app.upload.dir:uploads}")`：

```java
@Value("${app.upload.dir:uploads}")
private String uploadDir;
```

- [ ] **Step 7: 修改 removeById — 添加图片清理**

在 `removeById` 方法的步骤 6（版本历史清理）之后、步骤 7（删除 article）之前，添加：

```java
// 6.5. Clean up article images
List<ArticleImage> articleImages = articleImageMapper.selectByArticleId(articleId);
for (ArticleImage img : articleImages) {
    try {
        File file = new File(uploadDir, img.getUrl().replace("/uploads/", ""));
        if (file.exists()) file.delete();
    } catch (Exception e) {
        log.warn("Failed to delete image file: {}", img.getUrl(), e);
    }
}
articleImageMapper.deleteByArticleId(articleId);
```

- [ ] **Step 8: 编译验证**

```bash
cd blog-backend && mvn compile -f pom.xml
```

- [ ] **Step 9: Commit**

```bash
git add blog-backend/src/main/java/com/blog/entity/Article.java blog-backend/src/main/java/com/blog/service/impl/ArticleServiceImpl.java
git commit -m "feat: populate coverImage/imageCount/images in article CRUD"
```

---

### Task 5: 后端 — 孤儿文件清理定时任务

**Files:**
- Create: `blog-backend/src/main/java/com/blog/task/ImageCleanupScheduler.java`

- [ ] **Step 1: 创建 ImageCleanupScheduler**

```java
package com.blog.task;

import com.blog.mapper.ArticleImageMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ImageCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImageCleanupScheduler.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Autowired
    private ArticleImageMapper articleImageMapper;

    /**
     * 每天凌晨 3:00 清理孤儿图片文件（超过 24 小时未关联到文章）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOrphanImages() {
        log.info("Starting orphan image cleanup...");
        File articleDir = new File(uploadDir, "articles");
        if (!articleDir.exists() || !articleDir.isDirectory()) {
            log.info("Article image directory does not exist, skipping cleanup.");
            return;
        }

        // Collect all URLs currently in DB
        Set<String> dbUrls = articleImageMapper.selectList(null).stream()
                .map(img -> img.getUrl().replace("/uploads/articles/", ""))
                .collect(Collectors.toSet());

        File[] files = articleDir.listFiles();
        if (files == null) {
            log.info("No files in article image directory.");
            return;
        }

        long cutoff = Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli();
        int deleted = 0;

        for (File file : files) {
            if (!file.isFile()) continue;
            String filename = file.getName();
            if (!dbUrls.contains(filename) && file.lastModified() < cutoff) {
                if (file.delete()) {
                    deleted++;
                    log.debug("Deleted orphan image: {}", filename);
                }
            }
        }

        log.info("Orphan image cleanup completed. Deleted {} files.", deleted);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd blog-backend && mvn compile -f pom.xml
```

- [ ] **Step 3: Commit**

```bash
git add blog-backend/src/main/java/com/blog/task/ImageCleanupScheduler.java
git commit -m "feat: add ImageCleanupScheduler for orphan article image cleanup"
```

---

### Task 6: 前端 — TypeScript 类型 + API 模块

**Files:**
- Modify: `blog-frontend/types/index.ts` (add coverImage, images, imageCount to Article)
- Modify: `blog-frontend/api/modules/article.ts` (add uploadArticleImage, update createArticle signature)

**Interfaces:**
- Produces: `Article.coverImage: string | null`, `Article.images: string[]`, `Article.imageCount: number`
- Produces: `uploadArticleImage(file: File): Promise<string>`

- [ ] **Step 1: 扩展 Article 类型**

在 `types/index.ts` 的 `Article` 接口中，`updatedAt` 之后添加：

```typescript
export interface Article {
  // ... 现有字段保持不变 ...
  coverImage: string | null
  images: string[]
  imageCount: number
}
```

完整 `Article` 接口变为：

```typescript
export interface Article {
  id: number
  title: string
  content: string
  category: string
  status: 'draft' | 'published'
  likeCount: number
  favoriteCount: number
  readCount: number
  commentCount: number
  authorId: number | null
  createdAt: string
  updatedAt: string
  coverImage: string | null
  images: string[]
  imageCount: number
}
```

- [ ] **Step 2: 新增上传函数 + 更新 create/update**

在 `api/modules/article.ts` 末尾添加：

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

需要在文件顶部新增 import `apiRequest`：

```typescript
import { api, apiRequest } from '~/api/index'
```

检查 `api/index.ts` 是否导出了 `apiRequest`。若当前只导出了 `api`，需要查看 `api/index.ts` 的具体导出。打开并确认后调整 import。

- [ ] **Step 3: 类型检查**

```bash
cd blog-frontend && npx nuxi typecheck
```

预期：0 errors。

- [ ] **Step 4: Commit**

```bash
git add blog-frontend/types/index.ts blog-frontend/api/modules/article.ts
git commit -m "feat: add coverImage/images/imageCount to Article type and uploadArticleImage API"
```

---

### Task 7: 前端 — ArticleCard 改造为封面图卡片

**Files:**
- Modify: `blog-frontend/components/article/ArticleCard.vue`

**Interfaces:**
- Consumes: `Article.coverImage`, `Article.imageCount` (from Task 6 types)
- Produces: 封面图 + 数量角标 + 自适应高度卡片

- [ ] **Step 1: 重写 ArticleCard 模板**

完整替换 `components/article/ArticleCard.vue`：

```vue
<template>
  <NCard
    class="article-card overflow-hidden transition-all duration-300 hover:shadow-md hover:-translate-y-1 cursor-pointer"
    :bordered="false"
    size="small"
    @click="navigateTo(`/article/${article.id}`)"
  >
    <!-- Cover Image -->
    <div v-if="article.coverImage" class="relative overflow-hidden bg-gray-100">
      <img
        :src="imageUrl(article.coverImage)"
        :alt="article.title"
        class="w-full block object-cover"
        :style="{ minHeight: '120px', maxHeight: '320px' }"
        loading="lazy"
        @error="imgFailed = true"
      />
      <!-- Multi-image count badge -->
      <span
        v-if="article.imageCount && article.imageCount > 1"
        class="absolute bottom-2 right-2 bg-black/60 text-white text-xs px-1.5 py-0.5 rounded"
      >
        +{{ article.imageCount - 1 }}
      </span>
    </div>

    <!-- No-image fallback -->
    <div
      v-else
      class="h-24 bg-gradient-to-br from-primary-pale to-primary/10 flex items-center justify-center text-3xl"
    >
      {{ coverEmoji }}
    </div>

    <div class="p-3">
      <!-- Title -->
      <h3 class="text-sm font-bold leading-snug line-clamp-2 mb-1 text-text-primary">
        {{ article.title }}
      </h3>

      <!-- Tags -->
      <div v-if="tags.length > 0" class="flex gap-1 mb-2 flex-wrap">
        <NTag
          v-for="tag in tags"
          :key="tag.id"
          size="tiny"
          :bordered="false"
          type="success"
          class="cursor-pointer"
          @click.stop="navigateTo(`/tag/${tag.id}`)"
        >
          {{ tag.name }}
        </NTag>
      </div>

      <!-- Author Row -->
      <div
        v-if="author"
        class="flex items-center gap-2 mt-2 pt-2 border-t border-gray-100 cursor-pointer"
        @click.stop="navigateTo(`/user/${article.authorId}`)"
      >
        <UserAvatar :username="author.nickname || author.username" :src="author.avatar" size="small" />
        <span class="text-xs text-text-secondary hover:text-primary transition-colors truncate">
          {{ author.nickname || author.username }}
        </span>
      </div>

      <!-- Meta -->
      <div class="flex items-center justify-between text-xs text-text-secondary mt-1">
        <div class="flex items-center gap-3">
          <span class="flex items-center gap-1">
            <NIcon size="14"><EyeOutline /></NIcon>
            {{ formatCount(article.readCount) }}
          </span>
          <span class="flex items-center gap-1">
            <NIcon size="14"><HeartOutline /></NIcon>
            {{ formatCount(article.likeCount) }}
          </span>
          <span class="flex items-center gap-1">
            <NIcon size="14"><ChatbubbleOutline /></NIcon>
            {{ formatCount(article.commentCount) }}
          </span>
        </div>
      </div>
    </div>
  </NCard>
</template>
```

- [ ] **Step 2: 更新 script 部分**

```vue
<script setup lang="ts">
import { NCard, NTag, NIcon } from 'naive-ui'
import { EyeOutline, HeartOutline, ChatbubbleOutline } from '@vicons/ionicons5'
import { getArticleTags } from '~/api/modules/article'
import { getUserProfile } from '~/api/modules/user'
import type { Article, Tag, UserProfile } from '~/types'

const API_BASE = 'http://localhost:8080'

const props = defineProps<{
  article: Article
}>()

const tags = ref<Tag[]>([])
const author = ref<UserProfile | null>(null)
const imgFailed = ref(false)

const emojis = ['📝', '🌿', '📷', '🎨', '🍃', '✨', '📖', '🌸', '🌲', '🖋️']

const coverEmoji = computed(() => {
  const hash = props.article.title.split('').reduce((a, c) => a + c.charCodeAt(0), 0)
  return emojis[hash % emojis.length]
})

function imageUrl(src: string): string {
  if (!src) return ''
  if (src.startsWith('http://') || src.startsWith('https://')) return src
  return API_BASE + src
}

function formatCount(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

async function fetchTags() {
  try {
    tags.value = await getArticleTags(props.article.id)
  } catch {
    // Non-critical, silently fail
  }
}

async function fetchAuthor() {
  if (!props.article.authorId) return
  try {
    author.value = await getUserProfile(props.article.authorId)
  } catch {
    // Non-critical
  }
}

onMounted(() => {
  fetchTags()
  fetchAuthor()
})

// Reset imgFailed when coverImage changes
watch(() => props.article.coverImage, () => {
  imgFailed.value = false
})
</script>
```

- [ ] **Step 3: 类型检查**

```bash
cd blog-frontend && npx nuxi typecheck
```

预期：0 errors。

- [ ] **Step 4: Commit**

```bash
git add blog-frontend/components/article/ArticleCard.vue
git commit -m "feat: redesign ArticleCard with cover image and count badge"
```

---

### Task 8: 前端 — 写文章页图片上传区

**Files:**
- Modify: `blog-frontend/pages/article/write.vue`

**Interfaces:**
- Consumes: `uploadArticleImage` (Task 6), `Article.images` type (Task 6)
- Produces: 图片上传区（即时上传、拖拽排序、删除、最多 9 张）

- [ ] **Step 1: 在标题 NFormItem 和标签 NFormItem 之间插入上传区模板**

在 `write.vue` 的 `<NFormItem path="title" ...>` 和 `<NFormItem path="tags" ...>` 之间添加：

```vue
<!-- Image Upload Area -->
<div class="mb-4">
  <label class="block text-sm font-medium mb-2" style="color: #333">
    图片（最多9张）
  </label>
  <div class="flex gap-3 flex-wrap">
    <!-- Uploaded thumbnails -->
    <div
      v-for="(img, idx) in uploadedImages"
      :key="idx"
      class="relative w-20 h-20 rounded-lg overflow-hidden bg-gray-100 flex-shrink-0 group"
      :class="{ 'opacity-50': uploadingStates[idx] }"
    >
      <img :src="img" class="w-full h-full object-cover" />
      <NSpin v-if="uploadingStates[idx]" size="tiny" class="absolute inset-0 m-auto" />
      <button
        v-else
        class="absolute top-0.5 right-0.5 w-5 h-5 rounded-full bg-black/50 text-white text-xs flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity"
        @click="removeImage(idx)"
      >
        ✕
      </button>
      <!-- Drag handle hint -->
      <div
        class="absolute bottom-0 left-0 right-0 h-5 bg-gradient-to-t from-black/30 to-transparent opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
      >
        <span class="text-white text-xs">{{ idx === 0 ? '封面' : idx + 1 }}</span>
      </div>
    </div>

    <!-- Add button -->
    <div
      v-if="uploadedImages.length < 9"
      class="w-20 h-20 rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center cursor-pointer hover:border-primary transition-colors bg-gray-50 flex-shrink-0"
      @click="triggerImageInput"
    >
      <span class="text-2xl text-gray-400">+</span>
      <span class="text-xs text-gray-400 mt-0.5">添加</span>
    </div>
  </div>
  <input
    ref="imageInputRef"
    type="file"
    accept="image/*"
    multiple
    class="hidden"
    @change="handleImageSelect"
  />
</div>
```

- [ ] **Step 2: 添加 script 中的图片相关逻辑**

在 `<script setup>` 中添加：

```typescript
// ========== 图片上传 ==========
const uploadedImages = ref<string[]>([])
const uploadingStates = ref<boolean[]>([])
const imageInputRef = ref<HTMLInputElement | null>(null)

function triggerImageInput() {
  imageInputRef.value?.click()
}

async function handleImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || files.length === 0) return

  const remaining = 9 - uploadedImages.value.length
  if (files.length > remaining) {
    message.warning(`最多再添加 ${remaining} 张图片`)
    input.value = '' // reset
    return
  }

  for (let i = 0; i < files.length; i++) {
    const file = files[i]

    // Client-side size check
    if (file.size > 5 * 1024 * 1024) {
      message.warning(`图片 "${file.name}" 超过 5MB 限制`)
      continue
    }

    const idx = uploadedImages.value.length
    uploadedImages.value.push('') // placeholder
    uploadingStates.value.push(true)

    try {
      // Show local preview first
      const localUrl = URL.createObjectURL(file)
      uploadedImages.value[idx] = localUrl

      const url = await uploadArticleImage(file)
      uploadedImages.value[idx] = url.startsWith('http')
        ? url
        : `http://localhost:8080${url}`
    } catch (err: any) {
      message.error(`上传 "${file.name}" 失败: ${err.message || '未知错误'}`)
      uploadedImages.value.splice(idx, 1)
      uploadingStates.value.splice(idx, 1)
    } finally {
      if (idx < uploadingStates.value.length) {
        uploadingStates.value[idx] = false
      }
    }
  }

  input.value = '' // reset for re-select
}

function removeImage(idx: number) {
  uploadedImages.value.splice(idx, 1)
  uploadingStates.value.splice(idx, 1)
}
```

在现有 import 中添加 `uploadArticleImage`：

```typescript
import { createArticle, updateArticle, getArticleById, getArticleTags, setArticleTags, uploadArticleImage } from '~/api/modules/article'
```

- [ ] **Step 3: 修改 saveArticle — 发送 imageUrls**

在 `saveArticle` 函数中，`createArticle` 或 `updateArticle` 调用时，将图片 URL 列表加入请求体。提取后端基础路径后的相对 URL：

```typescript
function getRelativeUrls(): string[] {
  return uploadedImages.value.map(u => {
    if (u.startsWith('http://localhost:8080')) return u.replace('http://localhost:8080', '')
    if (u.startsWith('blob:')) return '' // placeholder, will be skipped
    return u
  }).filter(u => u && !u.startsWith('blob:'))
}

async function saveArticle(status: 'draft' | 'published') {
  saving.value = true
  try {
    const category = getCategory()
    const imageUrls = getRelativeUrls()
    let articleId: number

    if (isEdit.value && editId.value) {
      await updateArticle(editId.value, { title: form.title, category, content: form.content, status, images: imageUrls })
      articleId = editId.value
    } else {
      const article = await createArticle({ title: form.title, category, content: form.content, status, images: imageUrls })
      articleId = article.id
    }
    // ... rest unchanged
```

- [ ] **Step 4: 草稿保存/恢复图片**

修改 `saveDraftToLocal` 保存图片：

```typescript
function saveDraftToLocal() {
  if (import.meta.client && authStore.user?.userId) {
    localStorage.setItem(draftKey.value, JSON.stringify({
      title: form.title,
      content: form.content,
      tagIds: selectedTagIds.value,
      images: uploadedImages.value.filter(u => !u.startsWith('blob:')),
    }))
  }
}
```

修改 `loadDraft` 恢复图片：

```typescript
function loadDraft() {
  if (import.meta.client) {
    const draft = localStorage.getItem(draftKey.value)
    if (draft) {
      try {
        const data = JSON.parse(draft)
        form.title = data.title || ''
        form.content = data.content || ''
        if (data.tagIds) selectedTagIds.value = data.tagIds
        if (data.images) uploadedImages.value = data.images
      } catch {}
    }
  }
}
```

修改 `fetchArticleForEdit` 恢复图片：

```typescript
async function fetchArticleForEdit() {
  try {
    const article = await getArticleById(editId.value!)
    form.title = article.title
    form.content = article.content
    const tags = await getArticleTags(editId.value!)
    selectedTagIds.value = tags.map(t => t.id)
    // Restore images
    if (article.images && article.images.length > 0) {
      uploadedImages.value = article.images.map(u =>
        u.startsWith('http') ? u : `http://localhost:8080${u}`
      )
    }
  } catch (err: any) {
    message.error('加载文章失败')
  }
}
```

- [ ] **Step 5: 在 saveArticle 中等待所有上传完成**

在提交文章前，检查 `uploadingStates` 是否全部完成：

```typescript
// In saveArticle, before creating/updating:
const stillUploading = uploadingStates.value.some(s => s)
if (stillUploading) {
  message.warning('图片还在上传中，请稍候...')
  saving.value = false
  return
}
```

- [ ] **Step 6: 类型检查**

```bash
cd blog-frontend && npx nuxi typecheck
```

预期：0 errors。

- [ ] **Step 7: Commit**

```bash
git add blog-frontend/pages/article/write.vue
git commit -m "feat: add image upload area to write page"
```

---

### Task 9: 前端 — 首页 JS Masonry 瀑布流布局

**Files:**
- Modify: `blog-frontend/pages/index.vue`

**Interfaces:**
- Consumes: `Article.coverImage`, `Article.imageCount` (Task 6)
- Consumes: `ArticleCard` (Task 7)

- [ ] **Step 1: 替换模板中的 grid 为 masonry 容器**

将 `index.vue` 模板中的：

```vue
<div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
  <ArticleCard v-for="article in articles" :key="article.id" :article="article" />
</div>
```

替换为：

```vue
<div ref="masonryRef" class="masonry-container relative" :style="{ height: containerHeight + 'px' }">
  <div
    v-for="(article, i) in articles"
    :key="article.id"
    class="masonry-item absolute"
    :style="positionStyles[i] || {}"
  >
    <ArticleCard :article="article" />
  </div>
</div>
```

同时更新骨架屏加载区域，保持骨架结构一致：

```vue
<div v-if="loading" class="masonry-container relative" style="height: 600px">
  <div
    v-for="i in 8"
    :key="i"
    class="masonry-item absolute"
    :style="skeletonPositions[i - 1] || {}"
  >
    <div class="skeleton rounded-card" :style="{ height: (140 + (i % 3) * 40) + 'px' }" />
  </div>
</div>
```

- [ ] **Step 2: 添加 Masonry 布局逻辑**

在 `<script setup>` 中添加：

```typescript
// ========== Masonry layout ==========
const masonryRef = ref<HTMLElement | null>(null)
const columnCount = ref(3)
const columnWidth = ref(300)
const gap = 16
const columnHeights = ref<number[]>([])
const containerHeight = ref(600)
const positionStyles = ref<Record<string, string>[]>([])
const skeletonPositions = ref<Record<string, string>[]>([])

function updateColumnCount() {
  if (!import.meta.client) return
  const w = window.innerWidth
  if (w < 640) columnCount.value = 2
  else if (w < 1024) columnCount.value = 3
  else columnCount.value = 4
  columnHeights.value = new Array(columnCount.value).fill(0)
  recalcColumnWidth()
}

function recalcColumnWidth() {
  if (!masonryRef.value) return
  const containerWidth = masonryRef.value.clientWidth
  if (containerWidth > 0) {
    columnWidth.value = (containerWidth - gap * (columnCount.value - 1)) / columnCount.value
  }
}

interface CardPosition {
  col: number
  top: number
}

const cardPositions = ref<CardPosition[]>([])

function recalcAllPositions() {
  if (columnCount.value === 0) return
  columnHeights.value = new Array(columnCount.value).fill(0)
  cardPositions.value = []

  for (let i = 0; i < articles.value.length; i++) {
    const article = articles.value[i]
    const cardHeight = estimateCardHeight(article)
    const col = shortestColumn()
    const top = columnHeights.value[col]
    cardPositions.value.push({ col, top })
    columnHeights.value[col] = top + cardHeight + gap
  }

  positionStyles.value = cardPositions.value.map(p => ({
    left: (p.col * (columnWidth.value + gap)) + 'px',
    top: p.top + 'px',
    width: columnWidth.value + 'px',
  }))

  containerHeight.value = Math.max(...columnHeights.value, 200)
}

function shortestColumn(): number {
  let minIdx = 0
  for (let i = 1; i < columnHeights.value.length; i++) {
    if (columnHeights.value[i] < columnHeights.value[minIdx]) minIdx = i
  }
  return minIdx
}

function estimateCardHeight(article: Article): number {
  // Base height without image
  let h = 120
  if (article.coverImage) {
    // Assume ~4:3 aspect ratio for cover images; actual height determined by CSS
    // Use a typical height: columnWidth * 0.75 + content
    h = columnWidth.value * 0.75 + 100
  }
  return h
}
```

- [ ] **Step 3: 绑定 resize + 重算**

替换或扩展现有的 `onMounted` 和 `watch`：

```typescript
function handleResize() {
  const oldCount = columnCount.value
  updateColumnCount()
  if (oldCount !== columnCount.value) {
    recalcAllPositions()
  }
}

// Replace or add to onMounted:
onMounted(() => {
  fetchHotArticles()
  fetchTags()
  fetchArticles().then(() => {
    nextTick(() => {
      updateColumnCount()
      recalcAllPositions()
    })
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  observer?.disconnect()
  window.removeEventListener('resize', handleResize)
})

// After articles are appended (in fetchArticles for page > 1):
// Recalc positions for newly appended items
watch([() => articles.value.length, activeTagId], () => {
  nextTick(() => {
    if (activeTagId.value) {
      // Reset and recalc all
      nextTick(() => {
        updateColumnCount()
        recalcAllPositions()
      })
    } else {
      recalcAllPositions()
    }
    setupObserver()
  })
})
```

- [ ] **Step 4: 骨架屏位置**

```typescript
// Static skeleton positions for 8 items in 3 columns
const skeletonPositions = ref<Record<string, string>[]>(
  Array.from({ length: 8 }, (_, i) => {
    const col = i % 3
    return {
      left: (col * 33.33) + '%',
      top: (Math.floor(i / 3) * 180) + 'px',
      width: 'calc(33.33% - 11px)',
    }
  })
)
```

- [ ] **Step 5: 类型检查**

```bash
cd blog-frontend && npx nuxi typecheck
```

预期：0 errors。

- [ ] **Step 6: Commit**

```bash
git add blog-frontend/pages/index.vue
git commit -m "feat: replace grid with JS masonry waterfall layout on homepage"
```

---

### Task 10: 前端 — 详情页图片轮播

**Files:**
- Modify: `blog-frontend/pages/article/[id].vue`

**Interfaces:**
- Consumes: `Article.images` (Task 6)

- [ ] **Step 1: 在详情页模板中（标签/正文上方）插入轮播区域**

在 `article/[id].vue` 中，「Stats Bar」和「Tags」之间插入：

```vue
<!-- Image Carousel -->
<div
  v-if="article.images && article.images.length > 0"
  class="relative mb-6 rounded-xl overflow-hidden bg-gray-100"
  style="height: 360px"
>
  <!-- Track -->
  <div
    ref="carouselTrack"
    class="flex overflow-x-auto snap-x snap-mandatory scrollbar-hide h-full"
    @scroll="onCarouselScroll"
  >
    <div
      v-for="(img, i) in article.images"
      :key="i"
      class="flex-shrink-0 w-full h-full snap-center flex items-center justify-center"
      @click="openLightbox(i)"
    >
      <img
        :src="carouselImageUrl(img)"
        class="max-w-full max-h-full object-contain cursor-pointer"
        :alt="`图片 ${i + 1}`"
        loading="lazy"
      />
    </div>
  </div>

  <!-- Prev/Next buttons (only when multiple images) -->
  <button
    v-if="article.images.length > 1 && currentSlide > 0"
    class="absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/80 shadow flex items-center justify-center hover:bg-white transition-colors"
    @click="slideTo(currentSlide - 1)"
  >
    <NIcon size="18"><ChevronBackOutline /></NIcon>
  </button>
  <button
    v-if="article.images.length > 1 && currentSlide < article.images.length - 1"
    class="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white/80 shadow flex items-center justify-center hover:bg-white transition-colors"
    @click="slideTo(currentSlide + 1)"
  >
    <NIcon size="18"><ChevronForwardOutline /></NIcon>
  </button>

  <!-- Dot indicators (only when multiple images) -->
  <div
    v-if="article.images.length > 1"
    class="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5"
  >
    <span
      v-for="(img, i) in article.images"
      :key="i"
      class="w-2 h-2 rounded-full transition-all duration-200"
      :class="i === currentSlide ? 'bg-white w-4' : 'bg-white/50'"
    />
  </div>
</div>
```

- [ ] **Step 2: 在 script 中添加轮播逻辑**

```typescript
// ========== Image Carousel ==========
const carouselTrack = ref<HTMLElement | null>(null)
const currentSlide = ref(0)

const API_BASE = 'http://localhost:8080'

function carouselImageUrl(src: string): string {
  if (!src) return ''
  if (src.startsWith('http://') || src.startsWith('https://')) return src
  return API_BASE + src
}

function onCarouselScroll() {
  if (!carouselTrack.value) return
  const track = carouselTrack.value
  const slideWidth = track.clientWidth
  if (slideWidth === 0) return
  currentSlide.value = Math.round(track.scrollLeft / slideWidth)
}

function slideTo(index: number) {
  if (!carouselTrack.value) return
  const slideWidth = carouselTrack.value.clientWidth
  carouselTrack.value.scrollTo({ left: slideWidth * index, behavior: 'smooth' })
  currentSlide.value = index
}

// ========== Lightbox (fullscreen preview) ==========
const lightboxVisible = ref(false)
const lightboxIndex = ref(0)

function openLightbox(index: number) {
  lightboxIndex.value = index
  lightboxVisible.value = true
}

function closeLightbox() {
  lightboxVisible.value = false
}
```

在 import 中添加：
```typescript
import { ChevronBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
```

- [ ] **Step 3: 添加全屏灯箱（Lightbox）模板**

在详情页模板底部（`</template>` 之前）添加：

```vue
<!-- Lightbox -->
<Teleport to="body">
  <div
    v-if="lightboxVisible"
    class="fixed inset-0 z-50 bg-black/90 flex items-center justify-center"
    @click="closeLightbox"
  >
    <button
      class="absolute top-4 right-4 w-10 h-10 rounded-full bg-white/20 text-white text-xl flex items-center justify-center hover:bg-white/30 transition-colors z-10"
      @click="closeLightbox"
    >
      ✕
    </button>

    <!-- Prev -->
    <button
      v-if="article.images && article.images.length > 1 && lightboxIndex > 0"
      class="absolute left-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/20 text-white flex items-center justify-center hover:bg-white/30 transition-colors z-10"
      @click.stop="lightboxIndex--"
    >
      <NIcon size="20"><ChevronBackOutline /></NIcon>
    </button>

    <!-- Image -->
    <img
      :src="carouselImageUrl(article.images?.[lightboxIndex] || '')"
      class="max-w-[90vw] max-h-[90vh] object-contain"
      @click.stop
    />

    <!-- Next -->
    <button
      v-if="article.images && article.images.length > 1 && lightboxIndex < article.images.length - 1"
      class="absolute right-4 top-1/2 -translate-y-1/2 w-10 h-10 rounded-full bg-white/20 text-white flex items-center justify-center hover:bg-white/30 transition-colors z-10"
      @click.stop="lightboxIndex++"
    >
      <NIcon size="20"><ChevronForwardOutline /></NIcon>
    </button>

    <!-- Counter -->
    <div
      v-if="article.images && article.images.length > 1"
      class="absolute bottom-6 left-1/2 -translate-x-1/2 text-white text-sm bg-black/40 px-3 py-1 rounded-full"
    >
      {{ lightboxIndex + 1 }} / {{ article.images.length }}
    </div>
  </div>
</Teleport>
```

- [ ] **Step 4: 添加隐藏滚动条样式**

在 `<style scoped>` 中添加（如果没有 `<style>` 块则新建）：

```vue
<style scoped>
.scrollbar-hide::-webkit-scrollbar {
  display: none;
}
.scrollbar-hide {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
```

- [ ] **Step 5: 类型检查**

```bash
cd blog-frontend && npx nuxi typecheck
```

预期：0 errors。

- [ ] **Step 6: Commit**

```bash
git add blog-frontend/pages/article/[id].vue
git commit -m "feat: add image carousel with lightbox to article detail page"
```

---

### Task 11: 集成验证 + CLAUDE.md 更新

**Files:**
- Modify: `CLAUDE.md`
- Modify: `blog-backend/CLAUDE.md`

- [ ] **Step 1: 端到端功能验证**

```bash
# 1. 启动后端
cd blog-backend && mvn spring-boot:run -f pom.xml

# 2. 新终端 — 启动前端
cd blog-frontend && npm run dev

# 3. 手动测试清单：
# □ 注册/登录 → 进入写文章页 → 看到图片上传区
# □ 上传1-9张图片 → 缩略图显示 → 可删除
# □ 保存草稿 → 刷新页面 → 图片仍显示
# □ 发布文章 → 跳转详情页 → 轮播正常显示
# □ 回到首页 → 卡片显示封面图 → 多图有"+N"角标
# □ 瀑布流布局正确 → 卡片有高有矮自然错落
# □ 无图文章 → 卡片回退为 emoji 样式
# □ 切换标签 → 瀑布流重置
# □ 删除文章 → 图片文件被清理
```

- [ ] **Step 2: 更新 CLAUDE.md**

在 `CLAUDE.md` 的「文章发布与草稿」部分后添加「文章图片系统」章节：

```markdown
### 文章图片系统（2026-06-21 新增）

- 每篇文章最多 9 张图片，独立存储于 `article_image` 表
- `POST /api/upload/article-image`：免鉴权上传端点，单张 ≤ 5MB，存入 `uploads/articles/`
- 文章列表 API 返回 `coverImage`（首图）和 `imageCount`（总图数）
- 文章详情 API 返回 `images: string[]`（全部图片 URL，按 sort_order 排序）
- 首页：JS Masonry 瀑布流布局，卡片高度根据封面图比例自适应
- ArticleCard：有图显示封面 + 右下角 +N 角标，无图回退为 emoji 占位
- 文章详情页：标题下方 CSS scroll-snap 横向轮播，360px 容器，支持全屏灯箱
- 写文章页：标题下方即時上传区，支持多选、拖拽排序、删除，草稿保留图片
- 级联删除：文章删除时清理 `article_image` 记录 + 服务器文件
- `ImageCleanupScheduler`：每天 3:00 清理 24h+ 未关联的孤儿图片文件
```

在 `blog-backend/CLAUDE.md` 更新端点列表和数据库表：

- 端点列表中新增：
  ```
  | POST | `/api/upload/article-image` | 上传文章配图（免鉴权，max 5MB） |
  ```

- 数据库表列表中新增：
  ```
  | `article_image` | 文章配图 | INDEX(article_id), sort_order 排序 |
  ```

- Redis key 设计中更新文章缓存说明（缓存中包含 images 字段）

- 文章删除级联清理中新增图片清理步骤

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md blog-backend/CLAUDE.md
git commit -m "docs: document article image system in CLAUDE.md"
```

---

### Task 12: 最终提交 + 推送

- [ ] **Step 1: 确认所有变更已提交**

```bash
git status
git log --oneline -15
```

- [ ] **Step 2: 推送到远端**

```bash
git push origin main
```
