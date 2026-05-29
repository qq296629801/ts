package com.ts.platform.contract;

import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayContractTest extends IntegrationTestBase {

    @Test
    void packages_public() throws Exception {
        mockMvc.perform(get("/api/v1/pay/packages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createOrder_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/pay/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_ok() throws Exception {
        User user = createUser("13900002001", "下单用户", 0);
        mockMvc.perform(post("/api/v1/pay/create-order")
                        .header("Authorization", bearerToken(user.getId(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").exists())
                .andExpect(jsonPath("$.data.codeUrl").exists());
    }
}
