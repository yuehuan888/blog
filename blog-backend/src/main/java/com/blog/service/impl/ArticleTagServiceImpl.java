package com.blog.service.impl;

import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Tag;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.TagMapper;
import com.blog.service.ArticleTagService;
import com.blog.util.AuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleTagServiceImpl implements ArticleTagService {

    private static final Logger log = LoggerFactory.getLogger(ArticleTagServiceImpl.class);
    private static final int MAX_TAGS_PER_ARTICLE = 5;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void setTags(Long articleId, List<Long> tagIds) {
        // Verify article exists and check permission
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new RuntimeException("Article not found: " + articleId);
        }

        Long currentUserId = AuthContext.getUserId();
        if (currentUserId == null) {
            throw new RuntimeException("Authentication required");
        }
        // Article author OR admin can set tags
        if (!AuthContext.isAdmin() && !article.getAuthorId().equals(currentUserId)) {
            throw new RuntimeException("Only the article author or admin can set tags");
        }

        if (tagIds == null || tagIds.isEmpty()) {
            removeAllTags(articleId);
            invalidateCaches();
            return;
        }

        if (tagIds.size() > MAX_TAGS_PER_ARTICLE) {
            throw new RuntimeException("Article can have at most " + MAX_TAGS_PER_ARTICLE + " tags");
        }

        // 校验所有 tagId 存在
        for (Long tagId : tagIds) {
            if (tagMapper.selectById(tagId) == null) {
                throw new RuntimeException("Tag not found: " + tagId);
            }
        }

        // 获取已有关联
        List<ArticleTag> existing = articleTagMapper.selectByArticleId(articleId);
        List<Long> existingTagIds = existing.stream().map(ArticleTag::getTagId).toList();

        // 被移除的标签 article_count -1
        for (Long existingTagId : existingTagIds) {
            if (!tagIds.contains(existingTagId)) {
                tagMapper.decrementArticleCount(existingTagId);
            }
        }

        // 删除所有旧关联
        articleTagMapper.deleteByArticleId(articleId);

        // 插入新关联
        for (Long tagId : tagIds) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);

            // 新增的标签 article_count +1
            if (!existingTagIds.contains(tagId)) {
                tagMapper.incrementArticleCount(tagId);
            }
        }

        invalidateCaches();
    }

    @Override
    public List<Tag> getByArticleId(Long articleId) {
        List<ArticleTag> links = articleTagMapper.selectByArticleId(articleId);
        if (links.isEmpty()) {
            return List.of();
        }
        List<Long> tagIds = links.stream().map(ArticleTag::getTagId).toList();
        return tagMapper.selectBatchIds(tagIds);
    }

    @Override
    @Transactional
    public void deleteByTagId(Long tagId) {
        articleTagMapper.deleteByTagId(tagId);
    }

    private void removeAllTags(Long articleId) {
        List<ArticleTag> existing = articleTagMapper.selectByArticleId(articleId);
        for (ArticleTag at : existing) {
            tagMapper.decrementArticleCount(at.getTagId());
        }
        articleTagMapper.deleteByArticleId(articleId);
    }

    private void invalidateCaches() {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete("tag::cloud:sort=count");
            redisTemplate.delete("tag::cloud:sort=hot");
        } catch (Exception e) {
            log.warn("Failed to invalidate tag cloud caches", e);
        }
    }
}
