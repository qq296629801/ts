CREATE TABLE t_invite_reward (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    inviter_id   BIGINT NOT NULL,
    invitee_id   BIGINT NOT NULL,
    reward_type  VARCHAR(32) NOT NULL,
    reward_quota INT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_invitee_reward (invitee_id, reward_type),
    INDEX idx_inviter (inviter_id)
);
