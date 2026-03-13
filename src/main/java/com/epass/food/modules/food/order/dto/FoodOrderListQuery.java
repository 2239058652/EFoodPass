package com.epass.food.modules.food.order.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FoodOrderListQuery extends PageQuery {

    /**
     * 订单编号，支持模糊查询
     */
    private String orderNo;

    /**
     * 下单用户ID
     */
    private Long userId;

    /**
     * 订单状态：10待确认 20制作中 30已完成 40已取消
     */
    private Integer orderStatus;
}