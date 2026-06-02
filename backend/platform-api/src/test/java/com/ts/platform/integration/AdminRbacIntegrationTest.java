package com.ts.platform.integration;

import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminRbacIntegrationTest extends IntegrationTestBase {

    @Test
    void userCannotAccessAdminBilling() throws Exception {
        User user = createUser("13900005001", "普通用户", 0);
        mockMvc.perform(get("/api/v1/admin/billing/orders")
                        .header("Authorization", bearerToken(user.getId(), "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessAdminUsers() throws Exception {
        User user = createUser("13900005002", "普通用户2", 0);
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", bearerToken(user.getId(), "USER")))
                .andExpect(status().isForbidden());
    }
}
