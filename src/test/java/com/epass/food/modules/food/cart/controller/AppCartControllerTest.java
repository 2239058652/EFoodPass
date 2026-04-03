package com.epass.food.modules.food.cart.controller;

import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutResponse;
import com.epass.food.modules.food.cart.dto.AppCartItemResponse;
import com.epass.food.modules.food.cart.dto.AppCartResponse;
import com.epass.food.modules.food.cart.service.AppCartService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppCartControllerTest {

    @Mock
    private AppCartService appCartService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AppCartController controller = new AppCartController(appCartService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void detailShouldReturnCurrentUserCart() throws Exception {
        AppCartItemResponse item = new AppCartItemResponse();
        item.setFoodItemId(101L);
        item.setName("Rice");
        item.setAmount(new BigDecimal("18.00"));
        item.setAvailable(true);

        AppCartResponse response = new AppCartResponse();
        response.setItems(List.of(item));
        response.setTotalQuantity(2);
        response.setTotalAmount(new BigDecimal("18.00"));
        response.setInvalidItemCount(0);
        response.setCanCheckout(true);

        when(appCartService.getCurrentUserCart(7L)).thenReturn(response);

        mockMvc.perform(get("/app/cart")
                        .principal(loginAuthentication(7L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("Rice"));

        verify(appCartService).getCurrentUserCart(7L);
    }

    @Test
    void addItemShouldPassRequestToService() throws Exception {
        mockMvc.perform(post("/app/cart/item")
                        .principal(loginAuthentication(8L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodItemId": 11,
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(appCartService).addItem(eq(8L), argThat(request ->
                request != null
                        && Long.valueOf(11L).equals(request.getFoodItemId())
                        && Integer.valueOf(3).equals(request.getQuantity())
        ));
    }

    @Test
    void addItemShouldRejectInvalidQuantity() throws Exception {
        mockMvc.perform(post("/app/cart/item")
                        .principal(loginAuthentication(8L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "foodItemId": 11,
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appCartService);
    }

    @Test
    void checkoutShouldReturnSummary() throws Exception {
        AppCartCheckoutResponse response = new AppCartCheckoutResponse();
        response.setTotalQuantity(4);
        response.setTotalAmount(new BigDecimal("46.50"));
        when(appCartService.checkout(9L, null)).thenReturn(response);

        mockMvc.perform(post("/app/cart/checkout")
                        .principal(loginAuthentication(9L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(4))
                .andExpect(jsonPath("$.data.totalAmount").value(46.5));

        verify(appCartService).checkout(9L, null);
    }

    @Test
    void updateQuantityShouldPassPathAndBody() throws Exception {
        mockMvc.perform(put("/app/cart/item/22")
                        .principal(loginAuthentication(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5
                                }
                                """))
                .andExpect(status().isOk());

        verify(appCartService).updateQuantity(eq(10L), eq(22L), argThat(request ->
                request != null && Integer.valueOf(5).equals(request.getQuantity())
        ));
    }

    @Test
    void removeAndClearShouldPassCurrentUser() throws Exception {
        mockMvc.perform(delete("/app/cart/item/12")
                        .principal(loginAuthentication(6L)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/app/cart/clear")
                        .principal(loginAuthentication(6L)))
                .andExpect(status().isOk());

        verify(appCartService).removeItem(6L, 12L);
        verify(appCartService).clearCart(6L);
    }

    private UsernamePasswordAuthenticationToken loginAuthentication(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "tester", "Tester");
        return new UsernamePasswordAuthenticationToken(loginUser, null, List.of());
    }
}
