package com.epass.food.modules.ai.dto;

public record OrderToolItemResult(
        Long foodItemId,
        String foodName,
        Integer quantity,
        String amount
) {
}
