package com.epass.food.modules.ai.dto;

public record ItemToolDetailResult(
        String status,
        String message,
        Long itemId,
        Long categoryId,
        String name,
        String price,
        Integer stock,
        Integer saleStatusCode,
        String saleStatusLabel,
        String description
) {
}
