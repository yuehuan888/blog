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
