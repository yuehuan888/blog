package com.blog.event;

import com.blog.entity.ArticleRead;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleReadMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

import java.util.Map;

@Component
public class ArticleReadEventListener {

    private static final Logger log = LoggerFactory.getLogger(ArticleReadEventListener.class);
    private static final String CACHE_ARTICLE_PREFIX = "article::";

    @Autowired
    private ArticleReadMapper articleReadMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Async("readTaskExecutor")
    @EventListener
    public void handleReadEvent(ReadEvent event) {
        try {
            ArticleRead record = new ArticleRead();
            record.setArticleId(event.getArticleId());
            record.setUserId(event.getUserId());
            record.setIp(event.getIp());
            articleReadMapper.insert(record);

            articleMapper.incrementReadCount(event.getArticleId());

            // Invalidate article cache so next load gets fresh readCount
            if (redisTemplate != null) {
                try {
                    redisTemplate.delete(CACHE_ARTICLE_PREFIX + event.getArticleId());
                } catch (Exception e) {
                    log.warn("Failed to delete article cache: articleId={}", event.getArticleId(), e);
                }
            }

            if (rabbitTemplate != null) {
                try {
                    Map<String, Object> msg = Map.of(
                            "articleId", event.getArticleId(),
                            "userId", event.getUserId(),
                            "ip", event.getIp()
                    );
                    rabbitTemplate.convertAndSend("blog.exchange", "blog.article.read", msg);
                } catch (Exception e) {
                    log.warn("Failed to send MQ read event: articleId={}", event.getArticleId(), e);
                }
            }

            log.debug("Read recorded: articleId={}, userId={}", event.getArticleId(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to record read: articleId={}, userId={}", event.getArticleId(), event.getUserId(), e);
        }
    }
}
