package com.ts.platform.invite;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InviteRewardRepository extends JpaRepository<InviteReward, Long> {

    boolean existsByInviteeIdAndRewardType(Long inviteeId, String rewardType);
}
