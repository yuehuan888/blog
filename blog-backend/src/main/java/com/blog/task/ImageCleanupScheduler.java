package com.blog.task;

import com.blog.mapper.ArticleImageMapper;
import com.blog.entity.ArticleImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ImageCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ImageCleanupScheduler.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Autowired
    private ArticleImageMapper articleImageMapper;

    /**
     * 每天凌晨 3:00 清理孤儿图片文件（超过 24 小时未关联到文章）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOrphanImages() {
        log.info("Starting orphan image cleanup...");
        File articleDir = new File(uploadDir, "articles");
        if (!articleDir.exists() || !articleDir.isDirectory()) {
            log.info("Article image directory does not exist, skipping cleanup.");
            return;
        }

        // Collect all URLs currently in DB
        List<ArticleImage> allImages = articleImageMapper.selectList(null);
        Set<String> dbUrls = allImages.stream()
                .map(img -> img.getUrl().replace("/uploads/articles/", ""))
                .collect(Collectors.toSet());

        File[] files = articleDir.listFiles();
        if (files == null) {
            log.info("No files in article image directory.");
            return;
        }

        long cutoff = Instant.now().minus(24, ChronoUnit.HOURS).toEpochMilli();
        int deleted = 0;

        for (File file : files) {
            if (!file.isFile()) continue;
            String filename = file.getName();
            if (!dbUrls.contains(filename) && file.lastModified() < cutoff) {
                if (file.delete()) {
                    deleted++;
                    log.debug("Deleted orphan image: {}", filename);
                }
            }
        }

        log.info("Orphan image cleanup completed. Deleted {} files.", deleted);
    }
}
