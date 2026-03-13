package com.epass.food.modules.food.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AppOrderCreateRequest {

    private String remark;

    @Valid
    @NotEmpty(message = "订单明细不能为空")
    private List<FoodOrderItemRequest> items;
}