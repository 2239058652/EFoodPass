package com.epass.food.modules.food.order.dto;

import lombok.Data;

@Data
public class OrderStatusCountResponse {

    /**
     * 10待确认 20制作中 30已完成 40已取消
     */
    private Integer orderStatus;

    private Long orderCount;
}