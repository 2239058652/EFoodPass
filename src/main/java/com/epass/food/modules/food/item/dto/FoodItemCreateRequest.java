package com.epass.food.modules.food.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FoodItemCreateRequest {

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "菜品名称不能为空")
    private String name;

    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.00", message = "价格不能小于0")
    private BigDecimal price;

    @NotNull(message = "库存不能为空")
    @Min(value = 0, message = "库存不能小于0")
    private Integer stock;

    @NotNull(message = "上下架状态不能为空")
    private Integer isOnSale;

    private String description;
}