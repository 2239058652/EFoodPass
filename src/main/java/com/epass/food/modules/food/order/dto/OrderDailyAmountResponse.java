package com.epass.food.modules.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDailyAmountResponse {

    /**
     * yyyy-MM-dd
     */
    private String statDate;

    private BigDecimal totalAmount;
}