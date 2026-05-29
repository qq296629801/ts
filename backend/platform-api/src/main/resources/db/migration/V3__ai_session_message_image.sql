CREATE TABLE t_ai_session (
    id         VARCHAR(36)  PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(100),
    mode       VARCHAR(20)  NOT NULL DEFAULT 'CHAT',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_user (user_id, updated_at DESC)
);

CREATE TABLE t_ai_message (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id   VARCHAR(36)  NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    content_type VARCHAR(20)  NOT NULL,
    content      TEXT         NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_msg_session (session_id, created_at)
);

CREATE TABLE t_image (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    session_id VARCHAR(36),
    image_url  VARCHAR(500) NOT NULL,
    prompt     TEXT,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_image_user (user_id, created_at DESC)
);
