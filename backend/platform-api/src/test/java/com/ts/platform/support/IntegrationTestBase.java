package com.ts.platform.support;

import com.ts.platform.auth.SmsCodeService;
import com.ts.platform.contract.OpenApiContractTestBase;
import com.ts.platform.security.JwtTokenProvider;
import com.ts.platform.user.User;
import com.ts.platform.user.UserQuota;
import com.ts.platform.user.UserQuotaRepository;
import com.ts.platform.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

/**
 * 集成测试基类：H2 + Flyway，验证码服务 Mock。
 */
public abstract class IntegrationTestBase extends OpenApiContractTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserQuotaRepository quotaRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @MockBean
    protected SmsCodeService smsCodeService;

    protected void mockSmsOk() {
        doNothing().when(smsCodeService).send(anyString());
        doNothing().when(smsCodeService).verify(anyString(), anyString());
    }

    protected String bearerToken(Long userId, String role) {
        return "Bearer " + jwtTokenProvider.createToken(userId, role, false);
    }

    protected User createUser(String phone, String nickname, int balance) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode("Pass1234"));
        user.setInviteCode("T" + phone.substring(phone.length() - 5));
        userRepository.save(user);
        UserQuota quota = new UserQuota();
        quota.setUserId(user.getId());
        quota.setBalance(balance);
        quota.setTotalGranted(balance);
        quotaRepository.save(quota);
        return user;
    }
}
