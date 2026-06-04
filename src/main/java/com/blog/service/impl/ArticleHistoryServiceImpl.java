package com.blog.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.entity.Article;
import com.blog.entity.ArticleHistory;
import com.blog.mapper.ArticleHistoryMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.service.ArticleHistoryService;
import com.blog.util.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ArticleHistoryServiceImpl implements ArticleHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ArticleHistoryServiceImpl.class);
    private static final int MAX_VERSIONS = 20;

    @Autowired
    private ArticleHistoryMapper articleHistoryMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    public IPage<ArticleHistory> getHistory(Long articleId, int page, int size) {
        List<ArticleHistory> all = articleHistoryMapper.selectByArticleId(articleId);
        int total = all.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        Page<ArticleHistory> result = new Page<>(page, size, total);
        if (fromIndex < total) {
            result.setRecords(all.subList(fromIndex, toIndex));
        } else {
            result.setRecords(List.of());
        }
        return result;
    }

    @Override
    public ArticleHistory getDetail(Long historyId) {
        ArticleHistory history = articleHistoryMapper.selectById(historyId);
        if (history == null) {
            throw new RuntimeException("History not found: " + historyId);
        }
        return history;
    }

    @Override
    @Transactional
    public Article rollback(Long articleId, Long historyId) {
        if (!AuthContext.isAdmin()) {
            throw new RuntimeException("Admin access required");
        }

        ArticleHistory target = getDetail(historyId);
        if (!target.getArticleId().equals(articleId)) {
            throw new RuntimeException("History does not belong to this article");
        }

        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("Article not found: " + articleId);
        }

        // Save current state as history before rollback
        int nextVersion = articleHistoryMapper.getMaxVersionNo(articleId) + 1;
        ArticleHistory preRollback = new ArticleHistory();
        preRollback.setArticleId(articleId);
        preRollback.setTitle(article.getTitle());
        preRollback.setContent(article.getContent());
        preRollback.setCategory(article.getCategory());
        preRollback.setVersionNo(nextVersion);
        preRollback.setChangeType("ROLLBACK");
        articleHistoryMapper.insert(preRollback);

        // Overwrite article with target history content
        article.setTitle(target.getTitle());
        article.setContent(target.getContent());
        article.setCategory(target.getCategory());
        articleMapper.updateById(article);

        // Cleanup old versions
        articleHistoryMapper.deleteOldVersions(articleId, MAX_VERSIONS);

        // Invalidate cache
        deleteCache("article::" + articleId);

        log.info("Rollback: articleId={} to historyId={} (version {})", articleId, historyId, target.getVersionNo());
        return article;
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
