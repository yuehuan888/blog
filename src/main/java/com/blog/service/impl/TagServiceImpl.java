package com.blog.service.impl;

import com.blog.dto.TagCloudItem;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.service.TagService;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
public class TagServiceImpl implements TagService {

    private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

    private static final String CACHE_TAG = "tag::";
    private static final String CACHE_TAG_CLOUD = "tag::cloud:sort=";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration CLOUD_CACHE_TTL = Duration.ofMinutes(5);

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public Tag create(String name, Long userId) {
        requireAdmin(userId);
        if (tagMapper.selectByName(name) != null) {
            throw new RuntimeException("Tag name already exists: " + name);
        }
        Tag tag = new Tag();
        tag.setName(name);
        tag.setArticleCount(0);
        tag.setHotScore(0);
        tagMapper.insert(tag);
        deleteCache(CACHE_TAG_CLOUD + "count");
        deleteCache(CACHE_TAG_CLOUD + "hot");
        return tag;
    }

    @Override
    @Transactional
    public Tag update(Long id, String name, Long userId) {
        requireAdmin(userId);
        Tag tag = getById(id);
        Tag existing = tagMapper.selectByName(name);
        if (existing != null && !existing.getId().equals(id)) {
            throw new RuntimeException("Tag name already exists: " + name);
        }
        tag.setName(name);
        tagMapper.updateById(tag);
        deleteCache(CACHE_TAG + id);
        deleteCache(CACHE_TAG_CLOUD + "count");
        deleteCache(CACHE_TAG_CLOUD + "hot");
        return tag;
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        requireAdmin(userId);
        getById(id);
        tagMapper.deleteById(id);
        deleteCache(CACHE_TAG + id);
        deleteCache(CACHE_TAG_CLOUD + "count");
        deleteCache(CACHE_TAG_CLOUD + "hot");
    }

    @Override
    public Tag getById(Long id) {
        String key = CACHE_TAG + id;
        Tag cached = getCache(key, Tag.class);
        if (cached != null) {
            log.debug("Cache hit: {}", key);
            return cached;
        }
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("Tag not found: " + id);
        }
        setCache(key, tag);
        return tag;
    }

    @Override
    public List<TagCloudItem> getCloud(String sort) {
        String cacheKey = CACHE_TAG_CLOUD + sort;
        JavaType listType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, TagCloudItem.class);
        List<TagCloudItem> cached = getCache(cacheKey, listType);
        if (cached != null) {
            log.debug("Cache hit: {}", cacheKey);
            return cached;
        }

        List<Tag> tags;
        if ("hot".equals(sort)) {
            tags = tagMapper.selectWithHotScore();
        } else {
            tags = tagMapper.selectByArticleCountDesc();
        }

        List<TagCloudItem> result = tags.stream()
                .map(t -> new TagCloudItem(
                        t.getId(),
                        t.getName(),
                        t.getArticleCount() != null ? t.getArticleCount() : 0,
                        t.getHotScore() != null ? t.getHotScore() : 0))
                .toList();

        setCache(cacheKey, result, CLOUD_CACHE_TTL);
        return result;
    }

    // ==================== 管理员校验 ====================

    private void requireAdmin(Long userId) {
        if (userId == null || userId != 1) {
            throw new RuntimeException("Admin access required");
        }
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

    private void setCache(String key, Object value, Duration ttl) {
        if (redisTemplate == null) return;
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
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
}
