package com.epass.food.modules.food.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppCartCheckoutResponse {

    private Integer totalQuantity;

    private BigDecimal totalAmount;
}
