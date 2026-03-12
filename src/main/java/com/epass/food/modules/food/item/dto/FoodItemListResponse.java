package com.epass.food.modules.food.item.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class FoodItemListResponse {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String name;

    private BigDecimal price;

    private Integer stock;

    private Integer isOnSale;
}