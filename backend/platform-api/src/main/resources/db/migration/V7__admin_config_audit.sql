CREATE TABLE t_system_config (
    config_key   VARCHAR(64)  PRIMARY KEY,
    config_value TEXT         NOT NULL,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE t_audit_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    target_id   BIGINT      NOT NULL,
    result      VARCHAR(20) NOT NULL,
    score       DECIMAL(5,2),
    reason      VARCHAR(500),
    operator_id BIGINT,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_target (target_type, target_id)
);

INSERT INTO t_system_config (config_key, config_value) VALUES
('register_gift_quota', '3'),
('invite_invitee_bonus', '3'),
('invite_inviter_register_bonus', '5'),
('invite_inviter_pay_bonus', '10'),
('audit_auto_reject_threshold', '0.85');
