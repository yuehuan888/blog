package com.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.blog.dto.ArticleQueryDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private static final String CACHE_ARTICLE = "article::";
    private static final String CACHE_CATEGORY_STATS = "categoryStats";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

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

        setCache(key, article);
        return article;
    }

    @Override
    @Transactional
    public boolean save(Article entity) {
        boolean result = super.save(entity);
        deleteCache(CACHE_CATEGORY_STATS);
        sendEvent("created", entity);
        return result;
    }

    @Override
    @Transactional
    public boolean updateById(Article entity) {
        getById(entity.getId());
        boolean result = super.updateById(entity);
        deleteCache(CACHE_ARTICLE + entity.getId());
        deleteCache(CACHE_CATEGORY_STATS);
        sendEvent("updated", entity);
        return result;
    }

    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        Article article = getById(id);
        boolean result = super.removeById(id);
        deleteCache(CACHE_ARTICLE + id);
        deleteCache(CACHE_CATEGORY_STATS);
        sendEvent("deleted", article);
        return result;
    }

    @Override
    public IPage<Article> page(ArticleQueryDTO query) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            wrapper.eq(Article::getCategory, query.getCategory());
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq(Article::getStatus, query.getStatus());
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            wrapper.and(w -> w
                    .like(Article::getTitle, query.getKeyword())
                    .or()
                    .like(Article::getContent, query.getKeyword()));
        }
        wrapper.orderByDesc(Article::getCreatedAt);

        Page<Article> page = new Page<>(query.getPage(), query.getSize());
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public Article patch(Long id, Article partial) {
        getById(id);

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
            wrapper.set(Article::getUpdatedAt, LocalDateTime.now());
            baseMapper.update(wrapper);
        }

        Article updated = super.getById(id);
        deleteCache(CACHE_ARTICLE + id);
        deleteCache(CACHE_CATEGORY_STATS);
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

    private JavaType listMapType() {
        return objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class);
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
}
