package com.epass.food.modules.food.order.dto;

import lombok.Data;

@Data
public class OrderPaymentStatusCountResponse {

    private Integer paymentStatus;

    private String paymentStatusLabel;

    private Long orderCount;
}
