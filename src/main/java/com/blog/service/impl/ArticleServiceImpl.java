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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

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
            wrapper.set(Article::getUpdatedAt, java.time.LocalDateTime.now());
            baseMapper.update(wrapper);
        }
        return getById(id);
    }

    @Override
    public List<Map<String, Object>> categoryStatistics() {
        return baseMapper.countByCategory();
    }
}
