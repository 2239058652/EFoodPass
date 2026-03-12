package com.epass.food.modules.food.item.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FoodItemDetailResponse {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String name;

    private BigDecimal price;

    private Integer stock;

    private Integer isOnSale;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}