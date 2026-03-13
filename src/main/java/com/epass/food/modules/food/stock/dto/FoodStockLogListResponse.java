package com.epass.food.modules.food.stock.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FoodStockLogListResponse {

    private Long id;

    private Long foodItemId;

    private String foodItemName;

    /**
     * 1下单扣减 2取消回补 3后台调整
     */
    private Integer changeType;

    private Integer changeAmount;

    private Integer beforeStock;

    private Integer afterStock;

    private Long bizId;

    private String remark;

    private LocalDateTime createdAt;
}