CREATE TABLE t_user (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone        VARCHAR(20)  UNIQUE,
    email        VARCHAR(100) UNIQUE,
    nickname     VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    invite_code  VARCHAR(10)  NOT NULL UNIQUE,
    inviter_id   BIGINT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0封禁',
    login_fail_count INT      NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_inviter FOREIGN KEY (inviter_id) REFERENCES t_user(id)
);

CREATE TABLE t_user_quota (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL UNIQUE,
    balance       INT NOT NULL DEFAULT 0,
    total_granted INT NOT NULL DEFAULT 0,
    total_used    INT NOT NULL DEFAULT 0,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_quota_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);

CREATE TABLE t_quota_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    change_amount INT NOT NULL,
    reason        VARCHAR(32) NOT NULL,
    ref_type      VARCHAR(32),
    ref_id        VARCHAR(64),
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_quota_log_user (user_id, created_at)
);

CREATE TABLE t_quota_reservation (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reservation_user_status (user_id, status)
);
