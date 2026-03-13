package com.epass.food.modules.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderStatOverviewResponse {

    private Long totalOrderCount;

    private Long pendingOrderCount;

    private Long processingOrderCount;

    private Long completedOrderCount;

    private Long canceledOrderCount;

    private BigDecimal totalAmount;

    private BigDecimal completedAmount;
}