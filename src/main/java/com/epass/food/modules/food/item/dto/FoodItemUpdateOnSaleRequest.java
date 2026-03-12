package com.epass.food.modules.food.item.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodItemUpdateOnSaleRequest {

    @NotNull(message = "菜品ID不能为空")
    private Long itemId;

    @NotNull(message = "上下架状态不能为空")
    private Integer isOnSale;
}