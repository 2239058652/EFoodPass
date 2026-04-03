package com.epass.food.modules.food.order.controller;

import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.food.order.dto.AppOrderPreviewResponse;
import com.epass.food.modules.food.order.service.FoodOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppOrderControllerTest {

    @Mock
    private FoodOrderService foodOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AppOrderController controller = new AppOrderController(foodOrderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void previewShouldPassCurrentUserAndReturnServiceResult() throws Exception {
        AppOrderPreviewResponse response = new AppOrderPreviewResponse();
        response.setTotalQuantity(3);
        when(foodOrderService.previewCurrentUserOrder(eq(9L), argThat(request ->
                request != null
                        && "less sugar".equals(request.getRemark())
                        && request.getItems() != null
                        && request.getItems().size() == 2
        ))).thenReturn(response);

        mockMvc.perform(post("/app/order/preview")
                        .principal(loginAuthentication(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "less sugar",
                                  "items": [
                                    {
                                      "foodItemId": 11,
                                      "quantity": 1
                                    },
                                    {
                                      "foodItemId": 12,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalQuantity").value(3));

        verify(foodOrderService).previewCurrentUserOrder(eq(9L), argThat(request ->
                request != null
                        && "less sugar".equals(request.getRemark())
                        && request.getItems() != null
                        && request.getItems().size() == 2
        ));
    }

    @Test
    void previewShouldRejectEmptyItems() throws Exception {
        mockMvc.perform(post("/app/order/preview")
                        .principal(loginAuthentication(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(foodOrderService);
    }

    @Test
    void payShouldPassCurrentUserAndRequestToService() throws Exception {
        mockMvc.perform(post("/app/order/pay/15")
                        .principal(loginAuthentication(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "alipay"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(foodOrderService).payCurrentUserOrder(eq(9L), eq(15L), argThat(request ->
                request != null && "alipay".equals(request.getPaymentMethod())
        ));
    }

    @Test
    void payShouldRejectBlankPaymentMethod() throws Exception {
        mockMvc.perform(post("/app/order/pay/15")
                        .principal(loginAuthentication(9L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(foodOrderService);
    }

    private UsernamePasswordAuthenticationToken loginAuthentication(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "tester", "Tester");
        return new UsernamePasswordAuthenticationToken(loginUser, null, List.of());
    }
}
