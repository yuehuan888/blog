package com.blog.task;

import com.blog.mapper.ArticleReadMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class HotArticleScheduler {

    private static final Logger log = LoggerFactory.getLogger(HotArticleScheduler.class);

    private static final String HOT_ZSET_7 = "article:hot:7";
    private static final String HOT_ZSET_30 = "article:hot:30";

    @Autowired
    private ArticleReadMapper articleReadMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 * * * ?")
    public void rebuildHotZsets() {
        if (redisTemplate == null) {
            log.debug("Redis not available, skipping hot ZSET rebuild");
            return;
        }

        log.info("Rebuilding hot article ZSETs...");
        rebuildZset(HOT_ZSET_7, 7, 2);
        rebuildZset(HOT_ZSET_30, 30, 2);
        log.info("Hot article ZSETs rebuild complete");
    }

    private void rebuildZset(String key, int days, int ttlHours) {
        try {
            List<Map<String, Object>> rows = articleReadMapper.countReadsByArticleInRange(days);

            redisTemplate.delete(key);
            for (Map<String, Object> row : rows) {
                Long articleId = ((Number) row.get("article_id")).longValue();
                int count = ((Number) row.get("cnt")).intValue();
                redisTemplate.opsForZSet().add(key, articleId.toString(), count);
            }
            redisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
            log.info("Rebuilt ZSET {}: {} entries, TTL={}h", key, rows.size(), ttlHours);
        } catch (Exception e) {
            log.error("Failed to rebuild ZSET: key={}, days={}", key, days, e);
        }
    }
}
