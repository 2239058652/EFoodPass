package com.epass.food.modules.food.app.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AppMenuItemResponse {

    private Long id;

    private Long categoryId;

    private String categoryName;

    private String name;

    private BigDecimal price;

    private Integer stock;

    private boolean soldOut;

    private String description;
}
