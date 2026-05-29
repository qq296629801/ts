package com.ts.platform.integration;

import com.ts.platform.common.BusinessException;
import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void register_wrongVerifyCode() throws Exception {
        doNothing().when(smsCodeService).send(anyString());
        doThrow(new BusinessException(400, "验证码错误或已过期"))
                .when(smsCodeService).verify(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800138001","password":"Pass1234","nickname":"用户A","verifyCode":"000000"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void login_lockAfterFiveFailures() throws Exception {
        mockSmsOk();
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"13800138002","password":"Pass1234","nickname":"用户B","verifyCode":"123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"account\":\"13800138002\",\"password\":\"wrongPwd1\"}"))
                    .andExpect(jsonPath("$.code").value(401));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"13800138002\",\"password\":\"Pass1234\"}"))
                .andExpect(jsonPath("$.code").value(403));
    }
}
