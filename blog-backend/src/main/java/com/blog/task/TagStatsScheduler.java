package com.blog.task;

import com.blog.entity.Tag;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.TagMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TagStatsScheduler {

    private static final Logger log = LoggerFactory.getLogger(TagStatsScheduler.class);

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 */6 * * ?")
    public void reconcileTagCounts() {
        log.info("Reconciling tag article_counts (published only)...");
        List<Tag> allTags = tagMapper.selectList(null);
        for (Tag tag : allTags) {
            int actual = tagMapper.countPublishedArticles(tag.getId());
            if (actual != tag.getArticleCount()) {
                log.info("Correcting tag '{}': {} -> {}", tag.getName(), tag.getArticleCount(), actual);
                tag.setArticleCount(actual);
                tagMapper.updateById(tag);
            }
        }
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("tag::cloud:sort=count");
                redisTemplate.delete("tag::cloud:sort=hot");
            } catch (Exception e) {
                log.warn("Failed to invalidate tag cloud caches", e);
            }
        }
        log.info("Tag reconciliation complete");
    }
}
