package com.epass.food.modules.food.order.controller;

import com.epass.food.modules.food.order.service.FoodOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FoodOrderControllerTest {

    @Mock
    private FoodOrderService foodOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        FoodOrderController controller = new FoodOrderController(foodOrderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(
                        new ByteArrayHttpMessageConverter(),
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
                .setValidator(validator)
                .build();
    }

    @Test
    void refundShouldPassRequestToService() throws Exception {
        mockMvc.perform(put("/food/order/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": 18
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(foodOrderService).refundOrder(argThat(request ->
                request != null && Long.valueOf(18L).equals(request.getOrderId())
        ));
    }

    @Test
    void refundShouldRejectMissingOrderId() throws Exception {
        mockMvc.perform(put("/food/order/refund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(foodOrderService);
    }

    @Test
    void exportShouldReturnCsvAttachmentAndBindQuery() throws Exception {
        when(foodOrderService.exportOrders(argThat(query ->
                query != null
                        && "NO-1".equals(query.getOrderNo())
                        && Integer.valueOf(20).equals(query.getPaymentStatus())
                        && LocalDateTime.of(2026, 4, 1, 0, 0).equals(query.getCreatedAtStart())
                        && LocalDateTime.of(2026, 4, 3, 23, 59, 59).equals(query.getCreatedAtEnd())
        ))).thenReturn("orderNo\nNO-1\n".getBytes());

        mockMvc.perform(get("/food/order/export")
                        .param("orderNo", "NO-1")
                        .param("paymentStatus", "20")
                        .param("createdAtStart", "2026-04-01 00:00:00")
                        .param("createdAtEnd", "2026-04-03 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(content().bytes("orderNo\nNO-1\n".getBytes()));

        verify(foodOrderService).exportOrders(argThat(query ->
                query != null
                        && "NO-1".equals(query.getOrderNo())
                        && Integer.valueOf(20).equals(query.getPaymentStatus())
                        && LocalDateTime.of(2026, 4, 1, 0, 0).equals(query.getCreatedAtStart())
                        && LocalDateTime.of(2026, 4, 3, 23, 59, 59).equals(query.getCreatedAtEnd())
        ));
    }
}
