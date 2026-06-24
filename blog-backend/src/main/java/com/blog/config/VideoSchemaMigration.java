package com.blog.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Idempotent schema migration for video feature.
 * Runs at startup — checks INFORMATION_SCHEMA before each ALTER TABLE.
 * Safe to run repeatedly (each statement is idempotent).
 */
@Component
public class VideoSchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(VideoSchemaMigration.class);

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        try (Connection conn = dataSource.getConnection()) {
            log.info("Running video schema migration...");

            // 1. Add columns to article table (idempotent)
            addColumnIfNotExists(conn, "article", "type",
                    "VARCHAR(20) NOT NULL DEFAULT 'article' COMMENT 'article|video'");
            addColumnIfNotExists(conn, "article", "video_url",
                    "VARCHAR(500) NULL COMMENT 'video storage path'");
            addColumnIfNotExists(conn, "article", "thumbnail_url",
                    "VARCHAR(500) NULL COMMENT 'video thumbnail'");
            addColumnIfNotExists(conn, "article", "duration",
                    "INT NULL COMMENT 'video duration in seconds'");
            addColumnIfNotExists(conn, "article", "ai_summary",
                    "TEXT NULL COMMENT 'AI summary'");
            addColumnIfNotExists(conn, "article", "transcode_status",
                    "VARCHAR(20) NULL COMMENT 'pending|processing|done|failed'");

            // 2. Add index on type (idempotent)
            addIndexIfNotExists(conn, "article", "idx_type", "type");

            // 3. Create danmaku table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS danmaku (" +
                    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  article_id BIGINT NOT NULL," +
                    "  user_id BIGINT NOT NULL," +
                    "  content VARCHAR(200) NOT NULL," +
                    "  timestamp_sec DOUBLE NOT NULL," +
                    "  color VARCHAR(20) DEFAULT '#FFFFFF'," +
                    "  mode VARCHAR(20) DEFAULT 'scroll'," +
                    "  created_at DATETIME NOT NULL," +
                    "  INDEX idx_article_time (article_id, timestamp_sec)" +
                    ")"
                );
            }

            // 4. Create video_fingerprint table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS video_fingerprint (" +
                    "  sha256 CHAR(64) PRIMARY KEY," +
                    "  object_key VARCHAR(500) NOT NULL," +
                    "  article_id BIGINT NULL," +
                    "  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")"
                );
            }

            log.info("Video schema migration complete.");
        } catch (Exception e) {
            log.error("Video schema migration failed — manual intervention may be required", e);
            // Don't prevent application startup — tables might already exist
        }
    }

    private void addColumnIfNotExists(Connection conn, String table, String column, String definition) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, table, column)) {
                if (!rs.next()) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                        log.info("Added column {}.{}", table, column);
                    }
                } else {
                    log.debug("Column {}.{} already exists, skipping", table, column);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to add column {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void addIndexIfNotExists(Connection conn, String table, String indexName, String columns) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getIndexInfo(null, null, table, false, false)) {
                boolean exists = false;
                while (rs.next()) {
                    if (indexName.equals(rs.getString("INDEX_NAME"))) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("ALTER TABLE " + table + " ADD INDEX " + indexName + " (" + columns + ")");
                        log.info("Added index {} on {}.{}", indexName, table, columns);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to add index {}: {}", indexName, e.getMessage());
        }
    }
}
