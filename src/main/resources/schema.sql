CREATE TABLE IF NOT EXISTS article (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(255)  NOT NULL,
    content    TEXT          NOT NULL,
    category   VARCHAR(100),
    status     VARCHAR(20)   NOT NULL DEFAULT 'draft',
    created_at DATETIME      NOT NULL,
    updated_at DATETIME      NOT NULL
);
