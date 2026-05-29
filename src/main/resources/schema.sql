CREATE TABLE IF NOT EXISTS article (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    title          VARCHAR(255)  NOT NULL,
    content        TEXT          NOT NULL,
    category       VARCHAR(100),
    status         VARCHAR(20)   NOT NULL DEFAULT 'draft',
    like_count     INT           NOT NULL DEFAULT 0,
    favorite_count INT           NOT NULL DEFAULT 0,
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
