package com.blog.service.impl;

import com.blog.dto.ToggleResult;
import com.blog.entity.Article;
import com.blog.entity.ArticleFavorite;
import com.blog.mapper.ArticleFavoriteMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleFavoriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleFavoriteServiceImpl implements ArticleFavoriteService {

    private static final Logger log = LoggerFactory.getLogger(ArticleFavoriteServiceImpl.class);

    private static final String REDIS_KEY_PREFIX = "article:favorite:";

    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;

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

        boolean isFavorited;
        try {
            isFavorited = checkRedis(articleId, userId);
        } catch (Exception e) {
            log.warn("Redis unavailable, falling back to DB: articleId={}, userId={}", articleId, userId);
            isFavorited = checkDb(articleId, userId);
        }

        if (isFavorited) {
            unfavorite(articleId, userId);
            articleMapper.decrementFavoriteCount(articleId);
        } else {
            favorite(articleId, userId);
            articleMapper.incrementFavoriteCount(articleId);
        }

        // Read fresh count from DB, not cache
        Article fresh = articleMapper.selectById(articleId);
        boolean newState = !isFavorited;

        // Invalidate article cache so next load gets fresh count
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("article::" + articleId);
            } catch (Exception e) {
                log.warn("Failed to delete article cache: articleId={}", articleId, e);
            }
        }

        return new ToggleResult(newState, fresh.getFavoriteCount());
    }

    @Override
    public boolean isFavorited(Long articleId, Long userId) {
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
        return articleFavoriteMapper.selectByUserAndArticle(userId, articleId) != null;
    }

    private void favorite(Long articleId, Long userId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForSet().add(REDIS_KEY_PREFIX + articleId, userId.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to update Redis: articleId={}, userId={}", articleId, userId);
        }

        ArticleFavorite record = new ArticleFavorite();
        record.setUserId(userId);
        record.setArticleId(articleId);
        articleFavoriteMapper.insert(record);
    }

    private void unfavorite(Long articleId, Long userId) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForSet().remove(REDIS_KEY_PREFIX + articleId, userId.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to update Redis: articleId={}, userId={}", articleId, userId);
        }

        articleFavoriteMapper.deleteByUserAndArticle(userId, articleId);
    }
}
