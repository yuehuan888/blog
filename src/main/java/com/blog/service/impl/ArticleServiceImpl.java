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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    private final RabbitTemplate rabbitTemplate;

    public ArticleServiceImpl(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    @Cacheable(value = "article", key = "#id")
    public Article getById(Serializable id) {
        Article article = super.getById(id);
        if (article == null) {
            throw new RuntimeException("Article not found: " + id);
        }
        return article;
    }

    @Override
    @Transactional
    @CacheEvict(value = "categoryStats", allEntries = true)
    public boolean save(Article entity) {
        boolean result = super.save(entity);
        sendEvent("created", entity);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"article", "categoryStats"}, key = "#entity.id")
    public boolean updateById(Article entity) {
        getById(entity.getId()); // ensure exists
        boolean result = super.updateById(entity);
        sendEvent("updated", entity);
        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"article", "categoryStats"}, key = "#id")
    public boolean removeById(Serializable id) {
        Article article = getById(id); // ensure exists
        boolean result = super.removeById(id);
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
    @CacheEvict(value = {"article", "categoryStats"}, key = "#id")
    public Article patch(Long id, Article partial) {
        getById(id); // ensure exists

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
        sendEvent("updated", updated);
        return updated;
    }

    @Override
    @Cacheable(value = "categoryStats")
    public List<Map<String, Object>> categoryStatistics() {
        return baseMapper.countByCategory();
    }

    private void sendEvent(String type, Article article) {
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
