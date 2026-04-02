package com.epass.food.modules.ai.dto;

import java.util.List;

public record OrderToolDetailResult(
        String status,
        String message,
        Long orderId,
        String orderNo,
        Long userId,
        Integer orderStatusCode,
        String orderStatusLabel,
        String totalAmount,
        String remark,
        String createdAt,
        List<OrderToolItemResult> items
) {
}
