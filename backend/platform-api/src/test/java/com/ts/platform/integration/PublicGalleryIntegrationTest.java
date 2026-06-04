package com.ts.platform.integration;

import com.ts.platform.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicGalleryIntegrationTest extends IntegrationTestBase {

    @Test
    void publicGallery_noAuth_ok() throws Exception {
        mockMvc.perform(get("/api/v1/gallery/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.sort").value("hot"));
    }

    @Test
    void publicGallery_sortLatest_noFeaturedType() throws Exception {
        mockMvc.perform(get("/api/v1/gallery/public").param("sort", "latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sort").value("latest"));
        // 响应项仅 TEMPLATE，无人工精选 FEATURED
        mockMvc.perform(get("/api/v1/gallery/public"))
                .andExpect(jsonPath("$.data.items[?(@.type == 'FEATURED')]").isEmpty());
    }
}
