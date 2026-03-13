package com.epass.food.modules.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FoodOrderItemResponse {

    private Long foodItemId;

    private String foodNameSnapshot;

    private BigDecimal priceSnapshot;

    private Integer quantity;

    private BigDecimal amount;
}