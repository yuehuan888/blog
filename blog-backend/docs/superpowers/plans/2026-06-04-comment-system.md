# 评论系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task.

**Goal:** Add nested comment system with replies, likes, soft/hard delete, and sensitive word filtering.

**Architecture:** parent_id + reply_to for nesting, top-level comments paginated with 3 latest child replies preloaded. CommentLike toggle follows ArticleLike pattern. Article.comment_count maintained via atomic SQL. Sensitive words in application.yml.

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.6, MySQL

---

### Task 1: Schema + config

**Files:**
- Modify: `src/main/resources/schema.sql`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Add article.comment_count column**

In schema.sql article table, after `read_count` line:
```sql
    comment_count INT           NOT NULL DEFAULT 0,
```

- [ ] **Step 2: Add comment + comment_like tables**

Append to schema.sql:
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

- [ ] **Step 3: Add sensitive words config**

Append to application.yml:
```yaml
app:
  sensitive-words: 暴力,色情,赌博,毒品,诈骗
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/schema.sql src/main/resources/application.yml
git commit -m "feat: add comment tables and sensitive words config"
```

---

### Task 2: Entities

**Files:**
- Create: `src/main/java/com/blog/entity/Comment.java`
- Create: `src/main/java/com/blog/entity/CommentLike.java`
- Modify: `src/main/java/com/blog/entity/Article.java`

- [ ] **Step 1: Create Comment entity**

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
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long userId;

    private Long parentId;

    private Long replyTo;

    private String content;

    private Integer likeCount;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 2: Create CommentLike entity**

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
@TableName("comment_like")
public class CommentLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long commentId;

    private Long userId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: Add commentCount to Article**

After `private Integer readCount;` in Article.java:
```java
    private Integer commentCount;
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/blog/entity/Comment.java src/main/java/com/blog/entity/CommentLike.java src/main/java/com/blog/entity/Article.java
git commit -m "feat: add Comment, CommentLike entities and Article.commentCount"
```

---

### Task 3: DTOs and Mappers

**Files:**
- Create: `src/main/java/com/blog/dto/CommentDTO.java`
- Create: `src/main/java/com/blog/mapper/CommentMapper.java`
- Create: `src/main/java/com/blog/mapper/CommentLikeMapper.java`
- Modify: `src/main/java/com/blog/mapper/ArticleMapper.java`

- [ ] **Step 1: Create CommentDTO**

```java
package com.blog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {

    private Long id;
    private Long articleId;
    private Long userId;
    private Long parentId;
    private Long replyTo;
    private String content;
    private Integer likeCount;
    private String status;
    private LocalDateTime createdAt;
    private List<CommentDTO> replies;
    private int replyCount;
}
```

- [ ] **Step 2: Create CommentMapper**

```java
package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT * FROM comment WHERE article_id = #{articleId} AND parent_id IS NULL AND status = 'visible' ORDER BY #{orderBy} DESC")
    List<Comment> selectTopLevel(@Param("articleId") Long articleId, @Param("orderBy") String orderBy);

    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} AND status = 'visible' ORDER BY created_at ASC")
    List<Comment> selectReplies(@Param("parentId") Long parentId);

    @Select("SELECT * FROM comment WHERE parent_id = #{parentId} AND status = 'visible' ORDER BY created_at DESC LIMIT #{limit}")
    List<Comment> selectLatestReplies(@Param("parentId") Long parentId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM comment WHERE parent_id = #{parentId} AND status = 'visible'")
    int countReplies(@Param("parentId") Long parentId);

    @Update("UPDATE comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrementLikeCount(@Param("id") Long id);

    @Update("UPDATE comment SET like_count = like_count - 1 WHERE id = #{id} AND like_count > 0")
    int decrementLikeCount(@Param("id") Long id);
}
```

- [ ] **Step 3: Create CommentLikeMapper**

```java
package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.CommentLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT * FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    CommentLike selectByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @Delete("DELETE FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);
}
```

- [ ] **Step 4: Add commentCount methods to ArticleMapper**

Add to ArticleMapper.java:
```java
    @Update("UPDATE article SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrementCommentCount(@Param("id") Long id);

    @Update("UPDATE article SET comment_count = comment_count - 1 WHERE id = #{id} AND comment_count > 0")
    int decrementCommentCount(@Param("id") Long id);
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/blog/dto/CommentDTO.java src/main/java/com/blog/mapper/CommentMapper.java src/main/java/com/blog/mapper/CommentLikeMapper.java src/main/java/com/blog/mapper/ArticleMapper.java
git commit -m "feat: add CommentDTO, CommentMapper, CommentLikeMapper, Article comment_count methods"
```

---

### Task 4: CommentService interface + implementation

**Files:**
- Create: `src/main/java/com/blog/service/CommentService.java`
- Create: `src/main/java/com/blog/service/impl/CommentServiceImpl.java`

- [ ] **Step 1: Create CommentService interface**

```java
package com.blog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.CommentDTO;
import com.blog.dto.ToggleResult;
import com.blog.entity.Comment;

public interface CommentService {

    Comment create(Long articleId, Long parentId, Long replyTo, String content, Long userId);

    IPage<CommentDTO> getTopLevel(Long articleId, int page, int size, String sort);

    IPage<CommentDTO> getReplies(Long parentId, int page, int size);

    ToggleResult toggleLike(Long commentId, Long userId);

    void delete(Long commentId, Long userId);

    void hide(Long commentId, Long userId);
}
```

- [ ] **Step 2: Create CommentServiceImpl**

```java
package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.CommentDTO;
import com.blog.dto.ToggleResult;
import com.blog.entity.Comment;
import com.blog.entity.CommentLike;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentLikeMapper;
import com.blog.mapper.CommentMapper;
import com.blog.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);
    private static final int PRELOAD_REPLIES = 3;
    private static final String CACHE_COMMENT_PREFIX = "comment:article:";

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${app.sensitive-words:}")
    private String sensitiveWords;

    @Override
    @Transactional
    public Comment create(Long articleId, Long parentId, Long replyTo, String content, Long userId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new RuntimeException("Article not found: " + articleId);
        }
        if (parentId != null && commentMapper.selectById(parentId) == null) {
            throw new RuntimeException("Parent comment not found: " + parentId);
        }
        checkSensitiveWords(content);

        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyTo(replyTo);
        comment.setContent(content);
        comment.setLikeCount(0);
        comment.setStatus("visible");
        commentMapper.insert(comment);

        articleMapper.incrementCommentCount(articleId);
        invalidateCommentCache(articleId);

        return comment;
    }

    @Override
    public IPage<CommentDTO> getTopLevel(Long articleId, int page, int size, String sort) {
        String orderBy = "like".equals(sort) ? "like_count" : "created_at";
        List<Comment> topLevel = commentMapper.selectTopLevel(articleId, orderBy);

        int total = topLevel.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<CommentDTO> records = new ArrayList<>();
        if (fromIndex < total) {
            for (Comment c : topLevel.subList(fromIndex, toIndex)) {
                CommentDTO dto = toDTO(c);
                // Preload 3 latest replies
                List<Comment> latestReplies = commentMapper.selectLatestReplies(c.getId(), PRELOAD_REPLIES);
                List<CommentDTO> replyDTOs = new ArrayList<>();
                for (Comment r : latestReplies) {
                    replyDTOs.add(toDTO(r));
                }
                dto.setReplies(replyDTOs);
                dto.setReplyCount(commentMapper.countReplies(c.getId()));
                records.add(dto);
            }
        }

        Page<CommentDTO> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Override
    public IPage<CommentDTO> getReplies(Long parentId, int page, int size) {
        List<Comment> replies = commentMapper.selectReplies(parentId);
        int total = replies.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<CommentDTO> records = new ArrayList<>();
        if (fromIndex < total) {
            for (Comment c : replies.subList(fromIndex, toIndex)) {
                records.add(toDTO(c));
            }
        }

        Page<CommentDTO> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public ToggleResult toggleLike(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found: " + commentId);
        }

        CommentLike existing = commentLikeMapper.selectByCommentAndUser(commentId, userId);
        if (existing != null) {
            commentLikeMapper.deleteByCommentAndUser(commentId, userId);
            commentMapper.decrementLikeCount(commentId);
            return new ToggleResult(false, comment.getLikeCount() - 1);
        } else {
            CommentLike like = new CommentLike();
            like.setCommentId(commentId);
            like.setUserId(userId);
            commentLikeMapper.insert(like);
            commentMapper.incrementLikeCount(commentId);
            return new ToggleResult(true, comment.getLikeCount() + 1);
        }
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found: " + commentId);
        }

        if (userId == 1) {
            // Admin: recursive hard delete
            hardDeleteRecursive(commentId);
        } else if (comment.getUserId().equals(userId)) {
            // Owner: soft delete
            comment.setStatus("deleted");
            commentMapper.updateById(comment);
            articleMapper.decrementCommentCount(comment.getArticleId());
        } else {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        invalidateCommentCache(comment.getArticleId());
    }

    private void hardDeleteRecursive(Long commentId) {
        List<Comment> children = commentMapper.selectReplies(commentId);
        for (Comment child : children) {
            hardDeleteRecursive(child.getId());
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment != null) {
            commentMapper.deleteById(commentId);
            articleMapper.decrementCommentCount(comment.getArticleId());
        }
    }

    @Override
    @Transactional
    public void hide(Long commentId, Long userId) {
        if (userId == null || userId != 1) {
            throw new RuntimeException("Admin access required");
        }
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new RuntimeException("Comment not found: " + commentId);
        }
        comment.setStatus("hidden");
        commentMapper.updateById(comment);
        invalidateCommentCache(comment.getArticleId());
    }

    // ==================== helper methods ====================

    private void checkSensitiveWords(String content) {
        if (sensitiveWords == null || sensitiveWords.isBlank()) return;
        for (String word : sensitiveWords.split(",")) {
            String w = word.trim();
            if (!w.isEmpty() && content.contains(w)) {
                throw new RuntimeException("Comment contains sensitive word: " + w);
            }
        }
    }

    private CommentDTO toDTO(Comment c) {
        CommentDTO dto = new CommentDTO();
        dto.setId(c.getId());
        dto.setArticleId(c.getArticleId());
        dto.setUserId(c.getUserId());
        dto.setParentId(c.getParentId());
        dto.setReplyTo(c.getReplyTo());
        dto.setContent(c.getContent());
        dto.setLikeCount(c.getLikeCount());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setReplies(List.of());
        dto.setReplyCount(0);
        return dto;
    }

    private void invalidateCommentCache(Long articleId) {
        if (redisTemplate == null) return;
        try {
            String pattern = CACHE_COMMENT_PREFIX + articleId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to invalidate comment cache: articleId={}", articleId, e);
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/blog/service/CommentService.java src/main/java/com/blog/service/impl/CommentServiceImpl.java
git commit -m "feat: add CommentService with nested replies, like toggle, delete strategies"
```

---

### Task 5: CommentController

**Files:**
- Create: `src/main/java/com/blog/controller/CommentController.java`

- [ ] **Step 1: Create CommentController**

```java
package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.CommentDTO;
import com.blog.dto.Result;
import com.blog.dto.ToggleResult;
import com.blog.entity.Comment;
import com.blog.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comments")
    public Result<Comment> create(@RequestBody Comment comment,
                                   @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.ok(commentService.create(
                comment.getArticleId(),
                comment.getParentId(),
                comment.getReplyTo(),
                comment.getContent(),
                userId));
    }

    @GetMapping("/articles/{id}/comments")
    public Result<IPage<CommentDTO>> getTopLevel(@PathVariable Long id,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "time") String sort) {
        if (!"time".equals(sort) && !"like".equals(sort)) {
            return Result.fail(400, "sort must be 'time' or 'like'");
        }
        return Result.ok(commentService.getTopLevel(id, page, size, sort));
    }

    @GetMapping("/comments/{id}/replies")
    public Result<IPage<CommentDTO>> getReplies(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        return Result.ok(commentService.getReplies(id, page, size));
    }

    @PostMapping("/comments/{id}/like")
    public Result<ToggleResult> like(@PathVariable Long id,
                                      @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return Result.ok(commentService.toggleLike(id, userId));
    }

    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        commentService.delete(id, userId);
        return Result.ok();
    }

    @PutMapping("/comments/{id}/hide")
    public Result<Void> hide(@PathVariable Long id,
                              @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        commentService.hide(id, userId);
        return Result.ok();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/blog/controller/CommentController.java
git commit -m "feat: add CommentController with 6 endpoints"
```

---

### Task 6: Compile, test, update CLAUDE.md

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Compile**

```bash
cd D:/vibecoding1/blog-backend && mvn compile -f pom.xml
```
Expected: BUILD SUCCESS

- [ ] **Step 2: Update CLAUDE.md**

- Entity count: 7 → 9 (add Comment, CommentLike)
- Mapper count: 7 → 9
- Service count: 7 → 8
- Controller: add "CommentController（6 个端点）"
- DTO: add CommentDTO
- Database tables: add comment, comment_like
- API endpoints: add 6 new rows
- Add "评论系统" section: parent_id+reply_to nesting, preload 3 replies, delete strategy, sensitive words

- [ ] **Step 3: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md with comment system"
```

---

### Task 7: End-to-end test

- [ ] **Step 1: Start app**

```bash
mvn spring-boot:run -f pom.xml
```

- [ ] **Step 2: Create top-level comment**

```bash
curl -s -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 2" \
  --data-raw '{"articleId":13,"content":"Great article!"}'
```

- [ ] **Step 3: Create reply**

```bash
curl -s -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  --data-raw '{"articleId":13,"parentId":1,"replyTo":1,"content":"Thanks for the feedback!"}'
```

- [ ] **Step 4: Get comments with replies preloaded**

```bash
curl -s "http://localhost:8080/api/articles/13/comments?page=1&size=20&sort=time"
# Verify: top comment has replies array with the reply, replyCount=1
```

- [ ] **Step 5: Like a comment**

```bash
curl -s -X POST http://localhost:8080/api/comments/1/like -H "X-User-Id: 1"
# Expected: liked=true, count=1
```

- [ ] **Step 6: Test sensitive words**

```bash
curl -s -X POST http://localhost:8080/api/comments \
  -H "Content-Type: application/json" \
  --data-raw '{"articleId":13,"content":"This contains 暴力 content"}'
# Expected: 404 "Comment contains sensitive word: 暴力"
```

- [ ] **Step 7: Test soft delete (owner)**

```bash
curl -s -X DELETE http://localhost:8080/api/comments/2 -H "X-User-Id: 1"
# Reply author is userId=1, should soft delete
```

- [ ] **Step 8: Test admin hard delete**

```bash
curl -s -X DELETE http://localhost:8080/api/comments/1 -H "X-User-Id: 1"
# Admin (userId=1) hard deletes + cascades children
```

- [ ] **Step 9: Verify comment_count**

```bash
curl -s http://localhost:8080/api/articles/13/stats
# commentCount should be correct
```
