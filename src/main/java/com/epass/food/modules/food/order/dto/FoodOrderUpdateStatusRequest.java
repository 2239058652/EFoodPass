package com.epass.food.modules.food.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FoodOrderUpdateStatusRequest {

    @NotNull(message = "订单ID不能为空")
    private Long orderId;
}