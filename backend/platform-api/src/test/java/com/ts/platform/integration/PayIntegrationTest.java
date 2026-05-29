package com.ts.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ts.platform.pay.PayOrderRepository;
import com.ts.platform.pay.PayService;
import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import com.ts.platform.user.UserQuotaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PayService payService;

    @Autowired
    private PayOrderRepository orderRepository;

    @Autowired
    private UserQuotaRepository quotaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void payNotify_idempotentAndGrantQuota() throws Exception {
        User user = createUser("13900001001", "付费用户", 0);
        String token = bearerToken(user.getId(), "USER");

        String body = mockMvc.perform(post("/api/v1/pay/create-order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        String orderNo = objectMapper.readTree(body).path("data").path("orderNo").asText();

        String notify = "{\"notifyId\":\"n1\",\"orderNo\":\"" + orderNo + "\",\"transactionId\":\"wx1\"}";
        mockMvc.perform(post("/api/v1/pay/wx-notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notify))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/pay/wx-notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notify))
                .andExpect(status().isOk());

        int balance = quotaRepository.findByUserId(user.getId()).orElseThrow().getBalance();
        assertThat(balance).isEqualTo(10);

        mockMvc.perform(get("/api/v1/pay/order/" + orderNo).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    @Test
    void expirePendingOrder() {
        User user = createUser("13900001002", "超时用户", 0);
        var order = payService.createOrder(user.getId(), 1L);
        var entity = orderRepository.findByOrderNo((String) order.get("orderNo")).orElseThrow();
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        orderRepository.save(entity);

        int n = payService.expirePendingOrders();
        assertThat(n).isGreaterThanOrEqualTo(1);
        assertThat(orderRepository.findByOrderNo(entity.getOrderNo()).orElseThrow().getStatus())
                .isEqualTo("CANCELLED");
    }
}
