package com.epass.food.modules.food.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class FoodOrderCreateRequest {

    @NotNull(message = "下单用户ID不能为空")
    private Long userId;

    private String remark;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<FoodOrderItemRequest> items;
}