package com.epass.food.modules.food.item.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FoodItemSaleStatus {
    ON_SALE(1, "上架"),
    OFF_SALE(0, "下架");

    private final int code;
    private final String label;

    FoodItemSaleStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String getLabelByCode(Integer code) {
        if (code == null) {
            return "未知";
        }
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .map(FoodItemSaleStatus::getLabel)
                .findFirst()
                .orElse("未知");
    }
}