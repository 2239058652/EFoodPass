package com.epass.food.modules.food.cart.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppCartItemResponse {

    private Long foodItemId;

    private Long categoryId;

    private String categoryName;

    private String name;

    private BigDecimal price;

    private Integer quantity;

    private BigDecimal amount;

    private Integer stock;

    private boolean soldOut;

    private boolean available;

    private String unavailableReason;
}
