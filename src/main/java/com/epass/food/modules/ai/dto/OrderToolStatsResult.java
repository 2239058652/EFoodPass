package com.epass.food.modules.ai.dto;

public record OrderToolStatsResult(
        String status,
        String message,
        Long totalOrderCount,
        Long pendingOrderCount,
        Long processingOrderCount,
        Long completedOrderCount,
        Long canceledOrderCount,
        String totalAmount,
        String completedAmount
) {
}
