package com.epass.food.modules.food.order.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FoodOrderPaymentStatus {

    UNPAID(10, "待支付"),
    PAID(20, "已支付"),
    REFUNDED(30, "已退款");

    private final int code;
    private final String label;

    FoodOrderPaymentStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(status -> status.code == code);
    }

    public static String getLabelByCode(Integer code) {
        if (code == null) {
            return "未知";
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .map(FoodOrderPaymentStatus::getLabel)
                .findFirst()
                .orElse("未知");
    }
}
