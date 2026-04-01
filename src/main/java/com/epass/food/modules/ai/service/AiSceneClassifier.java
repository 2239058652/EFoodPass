package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiSceneType;
import org.springframework.stereotype.Component;

@Component
public class AiSceneClassifier {

    public AiSceneType classify(String message) {
        if (message == null || message.isBlank()) {
            return AiSceneType.GENERAL;
        }

        if (isOrderScene(message)) {
            return AiSceneType.ORDER;
        }

        if (isItemScene(message)) {
            return AiSceneType.ITEM;
        }

        if (isStockScene(message)) {
            return AiSceneType.STOCK;
        }

        if (isSystemScene(message)) {
            return AiSceneType.SYSTEM;
        }

        return AiSceneType.GENERAL;
    }

    private boolean isOrderScene(String message) {
        return message.contains("订单")
                || message.contains("下单")
                || message.contains("取消订单")
                || message.contains("订单状态");
    }

    private boolean isItemScene(String message) {
        return message.contains("菜品")
                || message.contains("商品")
                || message.contains("上架")
                || message.contains("下架");
    }

    private boolean isStockScene(String message) {
        return message.contains("库存")
                || message.contains("回补")
                || message.contains("扣减")
                || message.contains("库存日志");
    }

    private boolean isSystemScene(String message) {
        return message.contains("用户")
                || message.contains("角色")
                || message.contains("权限")
                || message.contains("登录");
    }
}