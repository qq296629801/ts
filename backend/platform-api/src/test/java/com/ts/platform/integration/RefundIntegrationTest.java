package com.ts.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ts.platform.pay.PayOrderRepository;
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

class RefundIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PayOrderRepository orderRepository;

    @Autowired
    private UserQuotaRepository quotaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminRefund_success_deductsQuota() throws Exception {
        User user = createUser("13900007001", "退款测试", 0);
        String userToken = bearerToken(user.getId(), "USER");
        String adminToken = bearerToken(99L, "ADMIN");

        String body = mockMvc.perform(post("/api/v1/pay/create-order")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1,\"payChannel\":\"WECHAT\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String orderNo = objectMapper.readTree(body).path("data").path("orderNo").asText();

        mockMvc.perform(post("/api/v1/pay/dev/simulate/" + orderNo).header("Authorization", userToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/billing/orders/" + orderNo + "/refund")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        var order = orderRepository.findByOrderNo(orderNo).orElseThrow();
        assertThat(order.getStatus()).isEqualTo("REFUNDED");
        var quota = quotaRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(quota.getBalance()).isZero();
    }

    @Test
    void refund_insufficientBalance_badRequest() throws Exception {
        User user = createUser("13900007002", "余额不足", 0);
        String userToken = bearerToken(user.getId(), "USER");
        String adminToken = bearerToken(99L, "ADMIN");

        String body = mockMvc.perform(post("/api/v1/pay/create-order")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String orderNo = objectMapper.readTree(body).path("data").path("orderNo").asText();
        mockMvc.perform(post("/api/v1/pay/dev/simulate/" + orderNo).header("Authorization", userToken))
                .andExpect(status().isOk());

        var quota = quotaRepository.findByUserId(user.getId()).orElseThrow();
        quota.setBalance(5);
        quotaRepository.save(quota);

        mockMvc.perform(post("/api/v1/admin/billing/orders/" + orderNo + "/refund")
                        .header("Authorization", adminToken))
                .andExpect(status().isBadRequest());

        assertThat(orderRepository.findByOrderNo(orderNo).orElseThrow().getStatus()).isEqualTo("PAID");
    }

    @Test
    void userCannotRefund_forbidden() throws Exception {
        User user = createUser("13900007003", "非管理员", 10);
        mockMvc.perform(post("/api/v1/admin/billing/orders/POFAKE/refund")
                        .header("Authorization", bearerToken(user.getId(), "USER")))
                .andExpect(status().isForbidden());
    }
}
