package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.HotArticleDTO;
import com.blog.entity.Article;
import com.blog.event.ReadEvent;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleReadMapper;
import com.blog.service.ArticleReadService;
import com.blog.service.ArticleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ArticleReadServiceImpl implements ArticleReadService {

    private static final Logger log = LoggerFactory.getLogger(ArticleReadServiceImpl.class);

    private static final String DEDUP_PREFIX = "article:read:";
    private static final String HOT_ZSET_PREFIX = "article:hot:";
    private static final Duration DEDUP_TTL = Duration.ofMinutes(30);

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleReadMapper articleReadMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public void recordRead(Long articleId, Long userId, String ip) {
        articleService.getById(articleId);

        boolean isNewRead;
        try {
            isNewRead = checkAndMarkDedup(articleId, userId);
        } catch (Exception e) {
            log.warn("Redis unavailable for dedup, counting as new read: articleId={}, userId={}", articleId, userId);
            isNewRead = true;
        }

        if (!isNewRead) {
            log.debug("Dedup hit: articleId={}, userId={} (within 30min)", articleId, userId);
            return;
        }

        try {
            incrementHotZsets(articleId);
        } catch (Exception e) {
            log.warn("Failed to update hot ZSET: articleId={}", articleId, e);
        }

        eventPublisher.publishEvent(new ReadEvent(articleId, userId, ip));
    }

    private boolean checkAndMarkDedup(Long articleId, Long userId) {
        if (redisTemplate == null) {
            throw new RuntimeException("Redis not available");
        }
        String key = DEDUP_PREFIX + articleId + ":" + userId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", DEDUP_TTL);
        return Boolean.TRUE.equals(success);
    }

    private void incrementHotZsets(Long articleId) {
        if (redisTemplate == null) return;
        String member = articleId.toString();
        redisTemplate.opsForZSet().incrementScore(HOT_ZSET_PREFIX + "7", member, 1);
        redisTemplate.opsForZSet().incrementScore(HOT_ZSET_PREFIX + "30", member, 1);
    }

    @Override
    public IPage<HotArticleDTO> getHotArticles(int days, int page, int size) {
        String zsetKey = HOT_ZSET_PREFIX + days;
        int start = (page - 1) * size;
        int end = start + size - 1;

        List<HotArticleDTO> result = new ArrayList<>();
        long total = 0;

        try {
            if (redisTemplate != null) {
                Long count = redisTemplate.opsForZSet().zCard(zsetKey);
                total = count != null ? count : 0;

                if (total >= start) {
                    Set<String> members = redisTemplate.opsForZSet()
                            .reverseRange(zsetKey, start, end);
                    if (members != null && !members.isEmpty()) {
                        List<Long> ids = members.stream().map(Long::valueOf).toList();
                        List<Article> articles = articleMapper.selectByIds(ids);
                        Map<Long, Article> articleMap = new java.util.LinkedHashMap<>();
                        for (Article a : articles) {
                            articleMap.put(a.getId(), a);
                        }
                        for (String member : members) {
                            Long id = Long.valueOf(member);
                            Article a = articleMap.get(id);
                            if (a != null) {
                                // Get score for exact count
                                Double score = redisTemplate.opsForZSet().score(zsetKey, member);
                                int readCount = score != null ? score.intValue() : 0;
                                result.add(new HotArticleDTO(id, a.getTitle(), readCount));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to query hot ZSET, falling back to DB", e);
        }

        if (result.isEmpty()) {
            result = queryHotFromDb(days);
            total = result.size();
            int fromIndex = Math.min(start, result.size());
            int toIndex = Math.min(end + 1, result.size());
            result = result.subList(fromIndex, toIndex);
        }

        Page<HotArticleDTO> pageResult = new Page<>(page, size, total);
        pageResult.setRecords(result);
        return pageResult;
    }

    private List<HotArticleDTO> queryHotFromDb(int days) {
        List<Map<String, Object>> rows = articleReadMapper.countReadsByArticleInRange(days);
        List<HotArticleDTO> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long articleId = ((Number) row.get("article_id")).longValue();
            int count = ((Number) row.get("cnt")).intValue();
            try {
                Article article = articleService.getById(articleId);
                result.add(new HotArticleDTO(articleId, article.getTitle(), count));
            } catch (Exception e) {
                log.warn("Article not found for hot list: id={}", articleId);
            }
        }
        return result;
    }
}
