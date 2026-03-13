package com.epass.food.modules.food.stock.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FoodStockLogListQuery extends PageQuery {

    private Long foodItemId;

    /**
     * 1下单扣减 2取消回补 3后台调整
     */
    private Integer changeType;
}