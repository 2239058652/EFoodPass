package com.epass.food.modules.food.item.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FoodItemListQuery extends PageQuery {

    /**
     * 菜品名称，支持模糊查询
     */
    private String name;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 上下架状态：1上架 0下架
     */
    private Integer isOnSale;
}