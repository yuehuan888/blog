package com.blog.task;

import com.blog.service.storage.VideoStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Clean up abandoned uploads: stale .part files (local) and expired Redis keys.
 * Runs daily at 3:00 AM.
 */
@Component
public class UploadCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(UploadCleanupScheduler.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Autowired
    private VideoStorageService videoStorageService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupStaleUploads() {
        log.info("Starting upload cleanup...");
        cleanupStalePartFiles();
        cleanupStaleRedisKeys();
        log.info("Upload cleanup complete");
    }

    /**
     * Clean up .part files older than 24h in uploads/videos/.
     */
    private void cleanupStalePartFiles() {
        Path videoDir = Paths.get(uploadDir, "videos");
        if (!Files.exists(videoDir)) return;

        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        try {
            Files.walkFileTree(videoDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (name.contains(".part") && attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        try {
                            Files.deleteIfExists(file);
                            log.info("Deleted stale part file: {}", file.getFileName());
                        } catch (IOException e) {
                            log.warn("Failed to delete stale part: {}", file, e);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("Failed to scan for stale part files", e);
        }
    }

    /**
     * Clean up stale Redis keys (TTL should handle most, this is belt-and-suspenders).
     */
    private void cleanupStaleRedisKeys() {
        if (redisTemplate == null) return;
        try {
            Set<String> keys = redisTemplate.keys("video:upload:*");
            if (keys != null) {
                for (String key : keys) {
                    Long ttl = redisTemplate.getExpire(key);
                    if (ttl != null && ttl < 0) {
                        // Key has no TTL (shouldn't happen, but clean it up)
                        redisTemplate.delete(key);
                        log.info("Deleted stale Redis key: {}", key);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to clean up stale Redis keys", e);
        }
    }
}
