package com.epass.food.modules.food.category.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FoodCategoryListQuery extends PageQuery {

    /**
     * 分类名称，支持模糊查询
     */
    private String name;

    /**
     * 状态：1启用 0停用
     */
    private Integer status;
}