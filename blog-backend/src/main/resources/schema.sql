CREATE TABLE IF NOT EXISTS article (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(255)  NOT NULL,
    content        TEXT          NOT NULL,
    category       VARCHAR(100),
    status         VARCHAR(20)   NOT NULL DEFAULT 'draft',
    like_count     INT           NOT NULL DEFAULT 0,
    favorite_count INT           NOT NULL DEFAULT 0,
    read_count     INT           NOT NULL DEFAULT 0,
    comment_count  INT           NOT NULL DEFAULT 0,
    author_id      BIGINT,
    created_at     DATETIME      NOT NULL,
    updated_at     DATETIME      NOT NULL
);

CREATE TABLE IF NOT EXISTS article_like (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_article (user_id, article_id)
);

CREATE TABLE IF NOT EXISTS article_favorite (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_article (user_id, article_id)
);

CREATE TABLE IF NOT EXISTS article_read (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    ip         VARCHAR(45),
    created_at DATETIME NOT NULL,
    INDEX idx_article_created (article_id, created_at),
    INDEX idx_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS tag (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    article_count INT          NOT NULL DEFAULT 0,
    hot_score     INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL,
    UNIQUE KEY uk_name (name)
);

CREATE TABLE IF NOT EXISTS article_tag (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_article_tag (article_id, tag_id)
);

CREATE TABLE IF NOT EXISTS article_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id  BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    category    VARCHAR(100),
    version_no  INT          NOT NULL,
    change_type VARCHAR(20)  NOT NULL DEFAULT 'UPDATE',
    created_at  DATETIME     NOT NULL,
    INDEX idx_article_version (article_id, version_no)
);

CREATE TABLE IF NOT EXISTS comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id  BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    parent_id   BIGINT,
    reply_to    BIGINT,
    content     TEXT         NOT NULL,
    like_count  INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'visible',
    created_at  DATETIME     NOT NULL,
    INDEX idx_article_parent (article_id, parent_id),
    INDEX idx_parent (parent_id)
);

CREATE TABLE IF NOT EXISTS comment_like (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    comment_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    created_at  DATETIME NOT NULL,
    UNIQUE KEY uk_comment_user (comment_id, user_id)
);

CREATE TABLE IF NOT EXISTS user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    nickname    VARCHAR(50),
    avatar      VARCHAR(500),
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'user',
    created_at  DATETIME     NOT NULL,
    UNIQUE KEY uk_username (username)
);

CREATE TABLE IF NOT EXISTS user_follow (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    follower_id  BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at   DATETIME NOT NULL,
    UNIQUE KEY uk_follower_following (follower_id, following_id),
    INDEX idx_follower (follower_id),
    INDEX idx_following (following_id)
);

CREATE TABLE IF NOT EXISTS article_image (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    url        VARCHAR(500) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_article_id (article_id)
);

-- =====================================================
-- Migration: Video feature (2026-06-24)
-- Add columns to article (idempotent via INFORMATION_SCHEMA check)
-- =====================================================

-- 1. type column
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'type') = 0,
  'ALTER TABLE article ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT ''article'' COMMENT ''article|video''',
  'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. video_url column
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'video_url') = 0,
  'ALTER TABLE article ADD COLUMN video_url VARCHAR(500) NULL COMMENT ''video storage path''',
  'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. thumbnail_url column
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'thumbnail_url') = 0,
  'ALTER TABLE article ADD COLUMN thumbnail_url VARCHAR(500) NULL COMMENT ''video thumbnail''',
  'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. duration column
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'duration') = 0,
  'ALTER TABLE article ADD COLUMN duration INT NULL COMMENT ''video duration in seconds''',
  'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. ai_summary column
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'ai_summary') = 0,
  'ALTER TABLE article ADD COLUMN ai_summary TEXT NULL COMMENT ''AI summary''',
  'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. transcode_status column
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND COLUMN_NAME = 'transcode_status') = 0,
  'ALTER TABLE article ADD COLUMN transcode_status VARCHAR(20) NULL COMMENT ''pending|processing|done|failed''',
  'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. Add index on type column
SET @sql = IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article' AND INDEX_NAME = 'idx_type') = 0,
  'ALTER TABLE article ADD INDEX idx_type (type)',
  'SELECT 1'
); PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Danmaku table
CREATE TABLE IF NOT EXISTS danmaku (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id    BIGINT        NOT NULL COMMENT 'video/article id',
    user_id       BIGINT        NOT NULL,
    content       VARCHAR(200)  NOT NULL COMMENT 'danmaku text',
    timestamp_sec DOUBLE        NOT NULL COMMENT 'position in video (seconds)',
    color         VARCHAR(20)   DEFAULT '#FFFFFF' COMMENT 'danmaku color',
    mode          VARCHAR(20)   DEFAULT 'scroll' COMMENT 'scroll|top|bottom',
    created_at    DATETIME      NOT NULL,
    INDEX idx_article_time (article_id, timestamp_sec)
);

-- Video fingerprint table (for dedup fallback)
CREATE TABLE IF NOT EXISTS video_fingerprint (
    sha256      CHAR(64)     PRIMARY KEY,
    object_key  VARCHAR(500)  NOT NULL,
    article_id  BIGINT        NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
);
