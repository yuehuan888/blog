package com.blog.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.dto.ArticleQueryDTO;
import com.blog.dto.HotArticleDTO;
import com.blog.dto.Result;
import com.blog.dto.ToggleResult;
import com.blog.entity.Article;
import com.blog.entity.ArticleHistory;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleFavoriteService;
import com.blog.service.ArticleHistoryService;
import com.blog.service.ArticleLikeService;
import com.blog.service.ArticleReadService;
import com.blog.service.ArticleService;
import com.blog.service.ArticleTagService;
import com.blog.service.task.VideoTaskService;
import com.blog.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleLikeService articleLikeService;
    private final ArticleFavoriteService articleFavoriteService;
    private final ArticleReadService articleReadService;
    private final ArticleTagService articleTagService;
    private final ArticleHistoryService articleHistoryService;
    private final VideoTaskService videoTaskService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private ArticleMapper articleMapper;

    @PostMapping
    public Result<Article> create(@RequestBody Article article) {
        article.setAuthorId(AuthContext.getUserId());
        articleService.save(article);
        return Result.ok(article);
    }

    @PostMapping("/batch")
    public Result<Void> createBatch(@RequestBody List<Article> articles) {
        for (Article a : articles) {
            a.setAuthorId(AuthContext.getUserId());
        }
        boolean result = articleService.saveBatch(articles);
        return result ? Result.ok() : Result.fail(400, "插入失败");
    }

    @GetMapping
    public Result<IPage<Article>> list(ArticleQueryDTO query) {
        return Result.ok(articleService.page(query));
    }

    @GetMapping("/{id}")
    public Result<Article> getById(@PathVariable Long id, HttpServletRequest request) {
        Article article = articleService.getById(id);
        articleReadService.recordRead(id, AuthContext.getUserId(), request.getRemoteAddr());
        return Result.ok(article);
    }

    @PutMapping("/{id}")
    public Result<Article> update(@PathVariable Long id, @RequestBody Article article) {
        article.setId(id);
        articleService.updateById(article);
        return Result.ok(articleService.getById(id));
    }

    @PatchMapping("/{id}")
    public Result<Article> patch(@PathVariable Long id, @RequestBody Article article) {
        return Result.ok(articleService.patch(id, article));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.removeById(id);
        return Result.ok();
    }

    @DeleteMapping
    public Result<Void> deleteBatch(@RequestParam List<Long> ids) {
        articleService.removeByIds(ids);
        return Result.ok();
    }

    @GetMapping("/statistics/category")
    public Result<List<Map<String, Object>>> categoryStatistics() {
        return Result.ok(articleService.categoryStatistics());
    }

    @PostMapping("/{id}/like")
    public Result<ToggleResult> like(@PathVariable Long id) {
        return Result.ok(articleLikeService.toggle(id, AuthContext.getUserId()));
    }

    @PostMapping("/{id}/favorite")
    public Result<ToggleResult> favorite(@PathVariable Long id) {
        return Result.ok(articleFavoriteService.toggle(id, AuthContext.getUserId()));
    }

    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        Article article = articleService.getById(id);
        Long currentUserId = AuthContext.getUserId();
        Map<String, Object> stats = new HashMap<>();
        stats.put("likeCount", article.getLikeCount());
        stats.put("favoriteCount", article.getFavoriteCount());
        stats.put("readCount", article.getReadCount());
        stats.put("commentCount", article.getCommentCount());
        if (currentUserId != null) {
            stats.put("liked", articleLikeService.isLiked(id, currentUserId));
            stats.put("favorited", articleFavoriteService.isFavorited(id, currentUserId));
        } else {
            stats.put("liked", false);
            stats.put("favorited", false);
        }
        return Result.ok(stats);
    }

    @PutMapping("/{id}/tags")
    public Result<Void> setTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {
        articleTagService.setTags(id, tagIds);
        return Result.ok();
    }

    @GetMapping("/{id}/tags")
    public Result<List<Tag>> getTags(@PathVariable Long id) {
        return Result.ok(articleTagService.getByArticleId(id));
    }

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
    public Result<Article> rollback(@PathVariable Long id, @PathVariable Long historyId) {
        return Result.ok(articleHistoryService.rollback(id, historyId));
    }

    @GetMapping("/hot")
    public Result<IPage<HotArticleDTO>> hot(@RequestParam(defaultValue = "7") int days,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(articleReadService.getHotArticles(days, page, size));
    }

    /** Trigger AI video analysis on demand. Returns immediately; analysis runs async.
     *
     *  Two modes:
     *  - First analysis (aiSummary is null): allow AI cache dedup (same video → reuse summary, save API cost)
     *  - Re-analysis  (aiSummary present):  force fresh API call, clear old caches + null DB so frontend polling waits */
    @PostMapping("/{id}/ai-summary")
    public Result<Map<String, String>> generateAiSummary(@PathVariable Long id) {
        Article article = articleService.getById(id);
        if (article == null) {
            return Result.fail(404, "Article not found");
        }
        if (!"video".equals(article.getType())) {
            return Result.fail(400, "AI summary is only available for video articles");
        }

        String videoUrl = article.getVideoUrl();
        if (videoUrl == null || videoUrl.isBlank()) {
            return Result.fail(400, "Video has no video file to analyze");
        }

        String objectKey = extractObjectKey(videoUrl);
        String fileHash = objectKey != null ? objectKey : videoUrl;

        boolean isReAnalysis = article.getAiSummary() != null;

        if (isReAnalysis) {
            // Re-analysis: blow away AI result cache + article cache, null DB summary
            // so the frontend polling loop sees null → keeps waiting → gets the fresh result
            if (redisTemplate != null) {
                try {
                    redisTemplate.delete("ai:summary:" + fileHash);
                    redisTemplate.delete("article::" + id);
                } catch (Exception ignored) {}
            }
            if (articleMapper != null) {
                try {
                    LambdaUpdateWrapper<Article> clearWrapper = new LambdaUpdateWrapper<>();
                    clearWrapper.eq(Article::getId, id)
                                .set(Article::getAiSummary, null)
                                .set(Article::getUpdatedAt, LocalDateTime.now());
                    articleMapper.update(null, clearWrapper);
                } catch (Exception ignored) {}
            }
        }
        // First analysis: keep AI cache intact for cross-article dedup; don't touch DB

        videoTaskService.submitAiAnalysis(id, videoUrl, fileHash,
                article.getTitle(), article.getContent(), article.getCategory(), isReAnalysis);

        return Result.ok(Map.of("message", "AI analysis started", "articleId", String.valueOf(id)));
    }

    /** Extract MinIO/local object key from video URL. */
    private String extractObjectKey(String videoUrl) {
        String prefix = "/uploads/videos/";
        if (videoUrl.startsWith(prefix)) {
            return videoUrl.substring(prefix.length());
        }
        return videoUrl;
    }
}
