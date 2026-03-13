package com.epass.food.modules.food.item.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodItemAdjustStockRequest {

    @NotNull(message = "菜品ID不能为空")
    private Long itemId;

    @NotNull(message = "新库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    private String remark;
}