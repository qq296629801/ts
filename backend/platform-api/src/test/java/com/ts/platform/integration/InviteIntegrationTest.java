package com.ts.platform.integration;

import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import com.ts.platform.user.UserQuotaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InviteIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserQuotaRepository quotaRepository;

    @Test
    void registerWithInviteCode_grantsBonus() throws Exception {
        mockSmsOk();
        User inviter = createUser("13800138100", "邀请人", 3);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800138101","password":"Pass1234","nickname":"被邀请","verifyCode":"123456","inviteCode":"%s"}
                                """.formatted(inviter.getInviteCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        User invitee = userRepository.findByPhone("13800138101").orElseThrow();
        int inviteeBalance = quotaRepository.findByUserId(invitee.getId()).orElseThrow().getBalance();
        // 注册赠送 3 + 被邀请奖励 3
        assertThat(inviteeBalance).isGreaterThanOrEqualTo(6);

        int inviterBalance = quotaRepository.findByUserId(inviter.getId()).orElseThrow().getBalance();
        assertThat(inviterBalance).isGreaterThanOrEqualTo(8);
    }
}
