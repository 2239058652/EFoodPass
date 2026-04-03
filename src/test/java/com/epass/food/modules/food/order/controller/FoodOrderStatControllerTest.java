package com.epass.food.modules.food.order.controller;

import com.epass.food.modules.food.order.dto.OrderPaymentStatusCountResponse;
import com.epass.food.modules.food.order.service.FoodOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FoodOrderStatControllerTest {

    @Mock
    private FoodOrderService foodOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        FoodOrderStatController controller = new FoodOrderStatController(foodOrderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void paymentStatusCountShouldBindTimeRangeAndReturnServiceResult() throws Exception {
        OrderPaymentStatusCountResponse response = new OrderPaymentStatusCountResponse();
        response.setPaymentStatus(20);
        response.setOrderCount(5L);
        when(foodOrderService.getOrderPaymentStatusCounts(argThat(query ->
                query != null
                        && LocalDateTime.of(2026, 4, 1, 0, 0).equals(query.getCreatedAtStart())
                        && LocalDateTime.of(2026, 4, 3, 23, 59, 59).equals(query.getCreatedAtEnd())
        ))).thenReturn(List.of(response));

        mockMvc.perform(get("/food/order/stat/payment-status-count")
                        .param("createdAtStart", "2026-04-01 00:00:00")
                        .param("createdAtEnd", "2026-04-03 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paymentStatus").value(20))
                .andExpect(jsonPath("$.data[0].orderCount").value(5));

        verify(foodOrderService).getOrderPaymentStatusCounts(argThat(query ->
                query != null
                        && LocalDateTime.of(2026, 4, 1, 0, 0).equals(query.getCreatedAtStart())
                        && LocalDateTime.of(2026, 4, 3, 23, 59, 59).equals(query.getCreatedAtEnd())
        ));
    }
}
