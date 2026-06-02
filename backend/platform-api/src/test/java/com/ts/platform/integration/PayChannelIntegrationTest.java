package com.ts.platform.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ts.platform.pay.PayOrderRepository;
import com.ts.platform.support.IntegrationTestBase;
import com.ts.platform.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PayChannelIntegrationTest extends IntegrationTestBase {

    @Autowired
    private PayOrderRepository orderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void alipayOrder_notifyIdempotent() throws Exception {
        User user = createUser("13900006001", "支付宝用户", 0);
        String token = bearerToken(user.getId(), "USER");

        String body = mockMvc.perform(post("/api/v1/pay/create-order")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1,\"payChannel\":\"ALIPAY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payType").value("ALIPAY"))
                .andReturn().getResponse().getContentAsString();

        String orderNo = objectMapper.readTree(body).path("data").path("orderNo").asText();

        String notify = "{\"notifyId\":\"ali-n1\",\"orderNo\":\"" + orderNo + "\",\"transactionId\":\"ali-t1\"}";
        mockMvc.perform(post("/api/v1/pay/alipay-notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notify))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/pay/alipay-notify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(notify))
                .andExpect(status().isOk());

        var order = orderRepository.findByOrderNo(orderNo).orElseThrow();
        assertThat(order.getStatus()).isEqualTo("PAID");
        assertThat(order.getChannelTradeNo()).isEqualTo("ali-t1");
    }

    @Test
    void invalidPayChannel_badRequest() throws Exception {
        User user = createUser("13900006002", "渠道测试", 0);
        mockMvc.perform(post("/api/v1/pay/create-order")
                        .header("Authorization", bearerToken(user.getId(), "USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":1,\"payChannel\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest());
    }
}
