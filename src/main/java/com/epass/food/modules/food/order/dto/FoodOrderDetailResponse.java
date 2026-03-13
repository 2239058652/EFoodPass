package com.epass.food.modules.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FoodOrderDetailResponse {

    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    /**
     * 10待确认 20制作中 30已完成 40已取消
     */
    private Integer orderStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<FoodOrderItemResponse> items;
}