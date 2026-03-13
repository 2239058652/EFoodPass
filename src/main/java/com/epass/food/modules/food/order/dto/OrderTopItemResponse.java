package com.epass.food.modules.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderTopItemResponse {

    private Long foodItemId;

    private String foodName;

    private Long totalQuantity;

    private BigDecimal totalAmount;
}