package com.blog.service.task;

/**
 * Async task abstraction for video processing.
 * Dev:  LocalVideoTaskService (Spring @Async, zero dependencies)
 * Prod: RabbitMQVideoTaskService (RabbitMQ, persistent queues)
 */
public interface VideoTaskService {

    /** Submit video transcode task (FFmpeg HLS). Runs asynchronously. */
    void submitTranscode(Long articleId, String videoUrl, String objectKey);

    /** Submit AI video analysis task (DashScope). title/description/category used as prompt context. */
    void submitAiAnalysis(Long articleId, String videoUrl, String fileHash,
                          String title, String description, String category, boolean force);
}
