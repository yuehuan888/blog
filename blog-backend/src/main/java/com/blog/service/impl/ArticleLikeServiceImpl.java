package com.blog.service.impl;

import com.blog.dto.ToggleResult;
import com.blog.entity.Article;
import com.blog.entity.ArticleLike;
import com.blog.mapper.ArticleLikeMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleLikeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {

    private static final Logger log = LoggerFactory.getLogger(ArticleLikeServiceImpl.class);

    private static final String REDIS_KEY_PREFIX = "article:like:";

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public ToggleResult toggle(Long articleId, Long userId) {
        // Verify article exists (bypass cache for fresh data)
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("Article not found: " + articleId);
        }

        boolean isLiked;
        try {
            isLiked = checkRedis(articleId, userId);
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to DB: articleId={}, userId={}", articleId, userId);
            isLiked = checkDb(articleId, userId);
        }

        if (isLiked) {
            unlike(articleId, userId);
            articleMapper.decrementLikeCount(articleId);
        } else {
            like(articleId, userId);
            articleMapper.incrementLikeCount(articleId);
        }

        // Read fresh count from DB, not cache
        Article fresh = articleMapper.selectById(articleId);
        boolean newState = !isLiked;

        // Invalidate article cache so next load gets fresh count
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("article::" + articleId);
            } catch (Exception e) {
                log.warn("Failed to delete article cache: articleId={}", articleId, e);
            }
        }

        return new ToggleResult(newState, fresh.getLikeCount());
    }

    @Override
    public boolean isLiked(Long articleId, Long userId) {
        try {
            return checkRedis(articleId, userId);
        } catch (Exception e) {
            return checkDb(articleId, userId);
        }
    }

    private boolean checkRedis(Long articleId, Long userId) {
        if (redisTemplate == null) {
            throw new RuntimeException("Redis not available");
        }
        String key = REDIS_KEY_PREFIX + articleId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, userId.toString()));
    }

    private boolean checkDb(Long articleId, Long userId) {
        return articleLikeMapper.selectByUserAndArticle(userId, articleId) != null;
    }

    private void like(Long articleId, Long userId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForSet().add(REDIS_KEY_PREFIX + articleId, userId.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to update Redis: articleId={}, userId={}", articleId, userId);
        }
        ArticleLike record = new ArticleLike();
        record.setUserId(userId);
        record.setArticleId(articleId);
        articleLikeMapper.insert(record);
    }

    private void unlike(Long articleId, Long userId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForSet().remove(REDIS_KEY_PREFIX + articleId, userId.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to update Redis: articleId={}, userId={}", articleId, userId);
        }

        articleLikeMapper.deleteByUserAndArticle(userId, articleId);
    }
}
