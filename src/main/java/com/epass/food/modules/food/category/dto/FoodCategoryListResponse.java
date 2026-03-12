package com.epass.food.modules.food.category.dto;

import lombok.Data;

@Data
public class FoodCategoryListResponse {

    /**
     * 分类ID
     */
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 排序号
     */
    private Integer sortNo;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;
}