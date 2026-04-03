package com.epass.food.modules.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FoodOrderListResponse {

    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    /**
     * 10待确认 20制作中 30已完成 40已取消
     */
    private Integer orderStatus;

    private String orderStatusLabel;

    private Integer paymentStatus;

    private String paymentStatusLabel;

    private String paymentMethod;

    private LocalDateTime paidAt;

    private String closeReason;

    private String closeReasonLabel;

    private LocalDateTime closedAt;

    private String remark;

    private LocalDateTime createdAt;
}
