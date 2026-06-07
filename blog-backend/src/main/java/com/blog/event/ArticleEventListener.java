package com.blog.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.rabbitmq-enabled", havingValue = "true")
public class ArticleEventListener {

    private static final Logger log = LoggerFactory.getLogger(ArticleEventListener.class);

    @RabbitListener(queues = "blog.article.queue")
    public void handleArticleEvent(Map<String, Object> event) {
        log.info("Article event received: type={}, articleId={}, title={}",
                event.get("type"), event.get("articleId"), event.get("title"));
    }
}
