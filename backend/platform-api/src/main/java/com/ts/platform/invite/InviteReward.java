package com.ts.platform.invite;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_invite_reward")
public class InviteReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    @Column(name = "invitee_id", nullable = false)
    private Long inviteeId;

    @Column(name = "reward_type", nullable = false, length = 32)
    private String rewardType;

    @Column(name = "reward_quota", nullable = false)
    private Integer rewardQuota;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getInviterId() { return inviterId; }
    public void setInviterId(Long inviterId) { this.inviterId = inviterId; }
    public Long getInviteeId() { return inviteeId; }
    public void setInviteeId(Long inviteeId) { this.inviteeId = inviteeId; }
    public String getRewardType() { return rewardType; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }
    public Integer getRewardQuota() { return rewardQuota; }
    public void setRewardQuota(Integer rewardQuota) { this.rewardQuota = rewardQuota; }
}
