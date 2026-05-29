package com.ts.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ts.platform.admin.SystemConfigRepository;
import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminConfigIntegrationTest extends IntegrationTestBase {

    @Autowired
    private SystemConfigRepository configRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanUpdateConfig_persistedInDb() throws Exception {
        User admin = createUser("19900009999", "管理员", 0);
        admin.setRole("ADMIN");
        userRepository.save(admin);
        String token = bearerToken(admin.getId(), "ADMIN");

        mockMvc.perform(put("/api/v1/admin/config")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"register_gift_quota\":\"5\"}"))
                .andExpect(status().isOk());

        String value = configRepository.findById("register_gift_quota").orElseThrow().getConfigValue();
        assertThat(value).isEqualTo("5");

        mockMvc.perform(get("/api/v1/admin/config").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.register_gift_quota").value("5"));
    }

    @Test
    void nonAdmin_forbidden() throws Exception {
        User user = createUser("13900004001", "普通用户", 3);
        mockMvc.perform(get("/api/v1/admin/config")
                        .header("Authorization", bearerToken(user.getId(), "USER")))
                .andExpect(status().isForbidden());
    }
}
