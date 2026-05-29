package com.ts.platform.contract;

import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TemplateContractTest extends IntegrationTestBase {

    @Test
    void listTemplates_public() throws Exception {
        mockMvc.perform(get("/api/v1/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void publish_requiresAuth() throws Exception {
        mockMvc.perform(post("/api/v1/templates/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"prompt\":\"p\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publish_ok() throws Exception {
        User user = createUser("13900003001", "模版作者", 3);
        mockMvc.perform(post("/api/v1/templates/publish")
                        .header("Authorization", bearerToken(user.getId(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"测试模版","prompt":"一只猫","categoryId":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }
}
