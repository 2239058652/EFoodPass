package com.epass.food.modules.food.order.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FoodOrderStatus {

    PENDING(10, "待确认"),
    PROCESSING(20, "制作中"),
    COMPLETED(30, "已完成"),
    CANCELED(40, "已取消");

    private final int code;
    private final String label;

    FoodOrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static boolean isValid(Integer code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(status -> status.code == code);
    }

    public static FoodOrderStatus fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知订单状态: " + code));
    }

    public static String getLabelByCode(Integer code) {
        if (code == null) {
            return "未知";
        }

        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .map(FoodOrderStatus::getLabel)
                .findFirst()
                .orElse("未知");
    }

}