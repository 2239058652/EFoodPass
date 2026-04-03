package com.epass.food.modules.food.order.dto;

import lombok.Data;

@Data
public class OrderStatusCountResponse {

    /**
     * 10寰呯‘璁?20鍒朵綔涓?30宸插畬鎴?40宸插彇娑?
     */
    private Integer orderStatus;

    private String orderStatusLabel;

    private Long orderCount;
}
