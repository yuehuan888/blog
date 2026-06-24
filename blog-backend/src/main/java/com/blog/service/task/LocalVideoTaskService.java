package com.blog.service.task;

import com.blog.entity.Article;
import com.blog.mapper.VideoFingerprintMapper;
import com.blog.service.storage.VideoStorageService;
import com.blog.service.DashScopeService;
import com.blog.service.VideoTranscodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.mapper.ArticleMapper;

import java.time.LocalDateTime;

/**
 * Local async task service — uses Spring @Async with thread pool.
 * Dev default. Tasks are lost on process restart (acceptable for dev).
 */
@Service
@ConditionalOnProperty(name = "app.queue.type", havingValue = "local", matchIfMissing = true)
public class LocalVideoTaskService implements VideoTaskService {

    private static final Logger log = LoggerFactory.getLogger(LocalVideoTaskService.class);

    @Autowired
    private VideoTranscodeService videoTranscodeService;

    @Autowired
    private DashScopeService dashScopeService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private VideoFingerprintMapper videoFingerprintMapper;

    @Autowired
    private VideoStorageService videoStorageService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Override
    @Async("videoTaskExecutor")
    public void submitTranscode(Long articleId, String videoUrl, String objectKey) {
        log.info("Starting transcode for article={}", articleId);
        try {
            updateTranscodeStatus(articleId, Article.TRANSCODE_PROCESSING);
            videoTranscodeService.transcode(articleId, objectKey);
            updateTranscodeStatus(articleId, Article.TRANSCODE_DONE);
            log.info("Transcode done for article={}", articleId);
        } catch (Exception e) {
            log.error("Transcode failed for article={}", articleId, e);
            updateTranscodeStatus(articleId, Article.TRANSCODE_FAILED);
        }
    }

    @Override
    @Async("videoTaskExecutor")
    public void submitAiAnalysis(Long articleId, String videoUrl, String fileHash,
                                  String title, String description, String category, boolean force) {
        log.info("Starting AI analysis for article={}, hash={}, force={}", articleId, fileHash, force);
        try {
            // Check AI cache (skip when force=true — user clicked "re-analyze")
            if (!force && fileHash != null && redisTemplate != null) {
                try {
                    String cached = redisTemplate.opsForValue().get("ai:summary:" + fileHash);
                    if (cached != null) {
                        log.info("AI cache hit for hash={}, skipping API call", fileHash);
                        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
                        wrapper.eq(Article::getId, articleId)
                               .set(Article::getAiSummary, cached)
                               .set(Article::getUpdatedAt, LocalDateTime.now());
                        articleMapper.update(null, wrapper);
                        deleteArticleCache(articleId);
                        return;
                    }
                } catch (Exception e) {
                    log.warn("Redis error during AI cache check, continuing", e);
                }
            }

            // Call DashScope with video metadata
            String summary = dashScopeService.analyzeVideo(videoUrl, title, description, category);

            if (summary != null) {
                // Update article
                LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(Article::getId, articleId)
                       .set(Article::getAiSummary, summary)
                       .set(Article::getUpdatedAt, LocalDateTime.now());
                articleMapper.update(null, wrapper);
                deleteArticleCache(articleId);

                // Cache AI result
                if (fileHash != null && redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set("ai:summary:" + fileHash, summary,
                                java.time.Duration.ofDays(30));
                    } catch (Exception e) {
                        log.warn("Failed to cache AI result", e);
                    }
                }
                log.info("AI analysis done for article={}", articleId);
            }
        } catch (Exception e) {
            log.error("AI analysis failed for article={}", articleId, e);
            // AI failure is non-blocking — video still plays fine
        }
    }

    private void deleteArticleCache(Long articleId) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete("article::" + articleId);
                log.debug("Deleted article cache: article::{}", articleId);
            } catch (Exception ignored) {}
        }
    }

    private void updateTranscodeStatus(Long articleId, String status) {
        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Article::getId, articleId)
               .set(Article::getTranscodeStatus, status)
               .set(Article::getUpdatedAt, LocalDateTime.now());
        articleMapper.update(null, wrapper);
    }
}
