package com.epass.food.modules.food.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AppOrderPayRequest {

    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;
}
