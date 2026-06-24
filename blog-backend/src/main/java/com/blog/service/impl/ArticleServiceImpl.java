package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.dto.ArticleQueryDTO;
import com.blog.entity.Article;
import com.blog.entity.ArticleHistory;
import com.blog.entity.ArticleRead;
import com.blog.entity.ArticleImage;
import com.blog.entity.ArticleTag;
import com.blog.mapper.*;
import com.blog.mapper.ArticleImageMapper;
import com.blog.service.ArticleService;
import com.blog.util.AuthContext;
import java.util.Objects;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private static final String CACHE_ARTICLE = "article::";
    private static final String CACHE_CATEGORY_STATS = "categoryStats";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final int MAX_HISTORY_VERSIONS = 20;

    @Autowired
    private ArticleHistoryMapper articleHistoryMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;

    @Autowired
    private ArticleReadMapper articleReadMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentLikeMapper commentLikeMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ArticleImageMapper articleImageMapper;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private com.blog.service.task.VideoTaskService videoTaskService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public boolean saveBatch(Collection<Article> entityList) {
        boolean result = super.saveBatch(entityList);
        deleteCache(CACHE_CATEGORY_STATS);
        return result;
    }

    @Override
    public Article getById(Serializable id) {
        String key = CACHE_ARTICLE + id;
        Article cached = getCache(key, Article.class);
        if (cached != null) {
            log.debug("Cache hit: {}", key);
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

    @Override
    @Transactional
    public boolean save(Article entity) {
        // Default type to 'article' for backward compatibility
        if (entity.getType() == null || entity.getType().isBlank()) {
            entity.setType(Article.TYPE_ARTICLE);
        }
        entity.setContent(sanitizeHtml(entity.getContent()));
        boolean result = super.save(entity);

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

        deleteCache(CACHE_CATEGORY_STATS);
        deleteTagCloudCache();
        sendEvent("created", entity);

        // Trigger async transcode task for video type (AI analysis is user-triggered)
        if (Article.TYPE_VIDEO.equals(entity.getType()) && videoTaskService != null) {
            String videoUrl = entity.getVideoUrl();
            String objectKey = videoUrl != null ? extractObjectKey(videoUrl) : null;
            if (videoUrl != null) {
                videoTaskService.submitTranscode(entity.getId(), videoUrl, objectKey);
            }
        }

        return result;
    }

    /** Extract objectKey from a video URL (e.g., /uploads/videos/abc123.mp4 → abc123.mp4). */
    private String extractObjectKey(String videoUrl) {
        if (videoUrl == null) return null;
        int lastSlash = videoUrl.lastIndexOf('/');
        return lastSlash >= 0 ? videoUrl.substring(lastSlash + 1) : videoUrl;
    }

    @Override
    @Transactional
    public boolean updateById(Article entity) {
        entity.setContent(sanitizeHtml(entity.getContent()));
        Article old = getById(entity.getId());

        // Update images if provided
        if (entity.getImages() != null) {
            List<ArticleImage> oldImages = articleImageMapper.selectByArticleId(entity.getId());
            Set<String> newUrls = new HashSet<>(entity.getImages());

            // Only delete files that are NOT in the new image list
            for (ArticleImage oldImg : oldImages) {
                if (!newUrls.contains(oldImg.getUrl())) {
                    try {
                        File file = new File(uploadDir, oldImg.getUrl().replace("/uploads/", ""));
                        if (file.exists()) file.delete();
                    } catch (Exception e) {
                        log.warn("Failed to delete image file: {}", oldImg.getUrl(), e);
                    }
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

        saveHistoryIfNeeded(old, entity);
        boolean result = super.updateById(entity);
        deleteCache(CACHE_ARTICLE + entity.getId());
        deleteCache(CACHE_CATEGORY_STATS);
        deleteTagCloudCache();
        sendEvent("updated", entity);
        return result;
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        Long articleId = (Long) id;

        // Permission: article author or admin
        Article article = super.getById(articleId);
        if (article == null) {
            throw new RuntimeException("Article not found: " + articleId);
        }
        Long currentUserId = AuthContext.getUserId();
        if (currentUserId == null) {
            throw new RuntimeException("Authentication required");
        }
        if (!AuthContext.isAdmin() && !article.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("Only the article author or admin can delete");
        }

        // 1. Clean up tag associations and decrement counts
        List<ArticleTag> articleTags = articleTagMapper.selectByArticleId(articleId);
        for (ArticleTag at : articleTags) {
            tagMapper.decrementArticleCount(at.getTagId());
        }
        articleTagMapper.deleteByArticleId(articleId);

        // 2. Clean up likes
        articleLikeMapper.deleteByArticleId(articleId);

        // 3. Clean up favorites
        articleFavoriteMapper.deleteByArticleId(articleId);

        // 4. Clean up read records
        articleReadMapper.delete(new LambdaQueryWrapper<ArticleRead>()
                .eq(ArticleRead::getArticleId, articleId));

        // 5. Clean up comments and their likes
        List<Long> commentIds = commentMapper.selectIdsByArticleId(articleId);
        if (!commentIds.isEmpty()) {
            commentLikeMapper.deleteByCommentIds(commentIds);
            commentMapper.deleteByArticleId(articleId);
        }

        // 6. Clean up version history
        articleHistoryMapper.delete(new LambdaQueryWrapper<ArticleHistory>()
                .eq(ArticleHistory::getArticleId, articleId));

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

        // 7. Delete the article itself
        boolean result = super.removeById(articleId);

        // 8. Clear all Redis caches
        deleteCache(CACHE_ARTICLE + articleId);
        deleteCache(CACHE_CATEGORY_STATS);
        deleteTagCloudCache();
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("article:like:" + articleId);
                redisTemplate.delete("article:favorite:" + articleId);
                redisTemplate.opsForZSet().remove("article:hot:7", articleId.toString());
                redisTemplate.opsForZSet().remove("article:hot:30", articleId.toString());
            } catch (Exception e) {
                log.warn("Failed to clear Redis keys for article: {}", articleId, e);
            }
        }

        sendEvent("deleted", article);
        return result;
    }

    @Override
    public IPage<Article> page(ArticleQueryDTO query) {
        // 按标签筛选走 JOIN 查询
        if (query.getTagId() != null) {
            Page<Article> page = new Page<>(query.getPage(), query.getSize());
            String tagStatus = query.getStatus() != null && !query.getStatus().isBlank()
                    ? query.getStatus() : Article.STATUS_PUBLISHED;
            IPage<Article> result = baseMapper.selectByTagId(page,
                    query.getTagId(),
                    query.getCategory(),
                    tagStatus,
                    query.getKeyword(),
                    query.getAuthorId(),
                    query.getType());

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

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            wrapper.eq(Article::getCategory, query.getCategory());
        }
        String status = query.getStatus();
        if (status != null && !status.isBlank()) {
            wrapper.eq(Article::getStatus, status);
        } else {
            wrapper.eq(Article::getStatus, Article.STATUS_PUBLISHED);
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w
                    .like(Article::getTitle, query.getKeyword())
                    .or()
                    .like(Article::getContent, query.getKeyword()));
        }
        if (query.getAuthorId() != null) {
            wrapper.eq(Article::getAuthorId, query.getAuthorId());
        }
        // Video/Article type filter
        if (query.getType() != null && !query.getType().isBlank()) {
            wrapper.eq(Article::getType, query.getType());
        }
        wrapper.orderByDesc(Article::getCreatedAt);

        Page<Article> page = new Page<>(query.getPage(), query.getSize());
        IPage<Article> result = baseMapper.selectPage(page, wrapper);

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
            wrapper.set(Article::getContent, sanitizeHtml(partial.getContent()));
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
            // Build projected new article for comparison
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

    @Override
    public List<Map<String, Object>> categoryStatistics() {
        List<Map<String, Object>> cached = getCache(CACHE_CATEGORY_STATS, listMapType());
        if (cached != null) {
            return cached;
        }
        List<Map<String, Object>> stats = baseMapper.countByCategory();
        setCache(CACHE_CATEGORY_STATS, stats);
        return stats;
    }

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

    // ==================== 缓存工具方法 ====================

    private <T> T getCache(String key, Class<T> clazz) {
        if (redisTemplate == null) return null;
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json != null ? objectMapper.readValue(json, clazz) : null;
        } catch (Exception e) {
            log.warn("Failed to read cache: key={}", key, e);
            return null;
        }
    }

    private <T> T getCache(String key, JavaType type) {
        if (redisTemplate == null) return null;
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json != null ? objectMapper.readValue(json, type) : null;
        } catch (Exception e) {
            log.warn("Failed to read cache: key={}", key, e);
            return null;
        }
    }

    private void setCache(String key, Object value) {
        if (redisTemplate == null) return;
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to write cache: key={}", key, e);
        }
    }

    private void deleteCache(String key) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete cache: key={}", key, e);
        }
    }

    private void deleteTagCloudCache() {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete("tag::cloud:sort=count");
            redisTemplate.delete("tag::cloud:sort=hot");
        } catch (Exception e) {
            log.warn("Failed to delete tag cloud cache", e);
        }
    }

    private JavaType listMapType() {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class);
    }

    // ==================== HTML 清洗 ====================

    /**
     * Sanitize HTML content from rich text editor.
     * Preserves formatting tags (color, font-size, font-family) while stripping dangerous elements.
     * Plain text passes through unchanged.
     */
    private String sanitizeHtml(String content) {
        if (content == null) return null;
        if (!content.trim().startsWith("<")) return content; // plain text, skip

        Safelist safelist = Safelist.relaxed()
                // Allow span for inline styles (color, font-size, font-family)
                .addTags("span", "h1", "h2", "h3", "mark", "hr")
                // Allow style attribute on span (for color, font-size, font-family)
                .addAttributes("span", "style")
                .addAttributes("mark", "style")
                // Allow class on all elements
                .addAttributes(":all", "class");

        return Jsoup.clean(content, safelist);
    }

    // ==================== MQ 事件 ====================

    private void sendEvent(String type, Article article) {
        if (rabbitTemplate == null) {
            return;
        }
        try {
            Map<String, Object> event = Map.of(
                    "type", type,
                    "articleId", article.getId(),
                    "title", article.getTitle(),
                    "timestamp", LocalDateTime.now().toString()
            );
            rabbitTemplate.convertAndSend("blog.exchange", "blog.article." + type, event);
        } catch (Exception e) {
            log.error("Failed to send RabbitMQ event: type={}, articleId={}", type, article.getId(), e);
        }
    }

    // ==================== 图片批量加载 ====================

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
}
