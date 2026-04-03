package com.epass.food.modules.food.cart.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AppCartResponse {

    private List<AppCartItemResponse> items;

    private Integer totalQuantity;

    private BigDecimal totalAmount;

    private Integer invalidItemCount;

    private boolean canCheckout;
}
