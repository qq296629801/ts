CREATE TABLE t_template_category (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    status     TINYINT      NOT NULL DEFAULT 1
);

CREATE TABLE t_template (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    prompt          TEXT         NOT NULL,
    cover_image_url VARCHAR(500),
    category_id     BIGINT,
    source_type     VARCHAR(10)  NOT NULL,
    creator_id      BIGINT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    reject_reason   VARCHAR(500),
    use_count       INT          NOT NULL DEFAULT 0,
    like_count      INT          NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tpl_status (status, created_at DESC),
    INDEX idx_tpl_category (category_id)
);

CREATE TABLE t_template_favorite (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_template (user_id, template_id)
);

INSERT INTO t_template_category (name, sort_order) VALUES
('人像', 1), ('风景', 2), ('创意', 3), ('动漫', 4);
