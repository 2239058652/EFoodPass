package com.epass.food.modules.food.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodCategoryUpdateStatusRequest {

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 状态：1启用 0停用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}