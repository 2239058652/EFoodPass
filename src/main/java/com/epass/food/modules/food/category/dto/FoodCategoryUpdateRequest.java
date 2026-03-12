package com.epass.food.modules.food.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodCategoryUpdateRequest {

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;

    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 状态：1启用 0停用
     */
    @NotNull(message = "状态不能为空")
    private Integer status;
}