package com.epass.food.modules.food.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FoodOrderUpdateStatusRequest {

    @NotNull(message = "璁㈠崟ID涓嶈兘涓虹┖")
    private Long orderId;

    @Size(max = 100, message = "鍏抽棴鍘熷洜涓嶈兘瓒呰繃100涓瓧")
    private String closeReason;
}
