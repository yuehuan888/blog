package com.blog.task;

import com.blog.mapper.ArticleHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HistoryCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(HistoryCleanupScheduler.class);
    private static final int MAX_VERSIONS = 20;

    @Autowired
    private ArticleHistoryMapper articleHistoryMapper;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOrphanHistories() {
        log.info("Running history cleanup...");
        List<Long> articleIds = articleHistoryMapper.selectList(null)
                .stream()
                .map(h -> h.getArticleId())
                .distinct()
                .toList();

        for (Long articleId : articleIds) {
            try {
                articleHistoryMapper.deleteOldVersions(articleId, MAX_VERSIONS);
            } catch (Exception e) {
                log.warn("Failed to cleanup history for articleId={}", articleId, e);
            }
        }
        log.info("History cleanup complete, processed {} articles", articleIds.size());
    }
}
