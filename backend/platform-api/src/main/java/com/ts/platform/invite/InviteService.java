package com.ts.platform.invite;

import com.ts.platform.quota.QuotaLog;
import com.ts.platform.quota.QuotaLogRepository;
import com.ts.platform.user.User;
import com.ts.platform.user.UserQuota;
import com.ts.platform.user.UserQuotaRepository;
import com.ts.platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class InviteService {

    private final UserRepository userRepository;
    private final UserQuotaRepository quotaRepository;
    private final QuotaLogRepository logRepository;
    private final InviteRewardRepository rewardRepository;
    private final int inviteeBonus;
    private final int inviterRegisterBonus;
    private final int inviterPayBonus;

    public InviteService(
            UserRepository userRepository,
            UserQuotaRepository quotaRepository,
            QuotaLogRepository logRepository,
            InviteRewardRepository rewardRepository,
            @Value("${app.invite.invitee-bonus:3}") int inviteeBonus,
            @Value("${app.invite.inviter-register-bonus:5}") int inviterRegisterBonus,
            @Value("${app.invite.inviter-pay-bonus:10}") int inviterPayBonus) {
        this.userRepository = userRepository;
        this.quotaRepository = quotaRepository;
        this.logRepository = logRepository;
        this.rewardRepository = rewardRepository;
        this.inviteeBonus = inviteeBonus;
        this.inviterRegisterBonus = inviterRegisterBonus;
        this.inviterPayBonus = inviterPayBonus;
    }

    @Transactional
    public int applyRegisterInvite(User invitee, String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            return 0;
        }
        User inviter = userRepository.findByInviteCode(inviteCode).orElse(null);
        if (inviter == null || inviter.getId().equals(invitee.getId())) {
            return 0;
        }
        invitee.setInviterId(inviter.getId());
        userRepository.save(invitee);
        grantQuota(invitee.getId(), inviteeBonus, "INVITEE_REGISTER");
        grantQuota(inviter.getId(), inviterRegisterBonus, "INVITER_REGISTER");
        saveReward(inviter.getId(), invitee.getId(), "INVITEE_REGISTER", inviteeBonus);
        saveReward(inviter.getId(), invitee.getId(), "INVITER_REGISTER", inviterRegisterBonus);
        return inviteeBonus;
    }

    @Transactional
    public void onFirstPay(Long inviteeId) {
        User invitee = userRepository.findById(inviteeId).orElse(null);
        if (invitee == null || invitee.getInviterId() == null) {
            return;
        }
        if (rewardRepository.existsByInviteeIdAndRewardType(inviteeId, "INVITER_FIRST_PAY")) {
            return;
        }
        grantQuota(invitee.getInviterId(), inviterPayBonus, "INVITER_FIRST_PAY");
        saveReward(invitee.getInviterId(), inviteeId, "INVITER_FIRST_PAY", inviterPayBonus);
    }

    public Map<String, Object> inviteInfo(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        String link = "https://yourdomain.com/register?invite_code=" + user.getInviteCode();
        return Map.of("inviteCode", user.getInviteCode(), "inviteLink", link);
    }

    private void grantQuota(Long userId, int amount, String reason) {
        UserQuota quota = quotaRepository.findByUserId(userId).orElseThrow();
        quota.setBalance(quota.getBalance() + amount);
        quota.setTotalGranted(quota.getTotalGranted() + amount);
        quota.touch();
        quotaRepository.save(quota);
        logRepository.save(QuotaLog.of(userId, amount, reason));
    }

    private void saveReward(Long inviterId, Long inviteeId, String type, int quota) {
        InviteReward r = new InviteReward();
        r.setInviterId(inviterId);
        r.setInviteeId(inviteeId);
        r.setRewardType(type);
        r.setRewardQuota(quota);
        rewardRepository.save(r);
    }
}
