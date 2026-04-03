package com.epass.food.modules.food.order.enums;

import java.util.Arrays;

public enum FoodOrderCloseReason {

    MANUAL_CANCEL("MANUAL_CANCEL", "\u540e\u53f0\u624b\u52a8\u53d6\u6d88"),
    USER_CANCEL("USER_CANCEL", "\u7528\u6237\u4e3b\u52a8\u53d6\u6d88"),
    PAYMENT_TIMEOUT_AUTO_CANCEL("PAYMENT_TIMEOUT_AUTO_CANCEL", "\u652f\u4ed8\u8d85\u65f6\u81ea\u52a8\u53d6\u6d88"),
    REFUND_CANCEL("REFUND_CANCEL", "\u540e\u53f0\u9000\u6b3e\u53d6\u6d88\u8ba2\u5355");

    private final String code;
    private final String label;

    FoodOrderCloseReason(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static String getLabelByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        return Arrays.stream(values())
                .filter(reason -> reason.code.equals(code))
                .map(FoodOrderCloseReason::getLabel)
                .findFirst()
                .orElse(code);
    }
}
