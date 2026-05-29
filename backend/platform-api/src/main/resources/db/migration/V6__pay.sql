CREATE TABLE t_pay_package (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    quota      INT          NOT NULL,
    price      DECIMAL(10,2) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    status     TINYINT      NOT NULL DEFAULT 1
);

CREATE TABLE t_pay_order (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no          VARCHAR(64)  NOT NULL UNIQUE,
    user_id           BIGINT       NOT NULL,
    package_id        BIGINT       NOT NULL,
    amount            DECIMAL(10,2) NOT NULL,
    quota_granted     INT          NOT NULL,
    pay_type          VARCHAR(20)  NOT NULL DEFAULT 'WECHAT',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    wx_transaction_id VARCHAR(64),
    code_url          VARCHAR(512),
    expires_at        DATETIME     NOT NULL,
    paid_at           DATETIME,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_user (user_id, created_at DESC),
    INDEX idx_order_status_expire (status, expires_at)
);

CREATE TABLE t_pay_notify_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    notify_id    VARCHAR(128) NOT NULL UNIQUE,
    order_no     VARCHAR(64)  NOT NULL,
    processed_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO t_pay_package (name, quota, price, sort_order) VALUES
('体验包', 10, 6.00, 1),
('基础包', 50, 25.00, 2),
('标准包', 150, 60.00, 3),
('专业包', 500, 150.00, 4);
