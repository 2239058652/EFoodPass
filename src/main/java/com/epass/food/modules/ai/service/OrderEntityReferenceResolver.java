package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiEntityReference;
import com.epass.food.modules.ai.dto.AiQueryIntent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OrderEntityReferenceResolver {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("订单\\s*(\\d+)");

    public AiEntityReference resolve(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = ORDER_ID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        Long orderId = Long.valueOf(matcher.group(1));
        return new AiEntityReference("order", orderId, resolveIntent(message));
    }

    private AiQueryIntent resolveIntent(String message) {
        if (message.contains("状态")) {
            return AiQueryIntent.STATUS;
        }
        if (message.contains("金额")) {
            return AiQueryIntent.AMOUNT;
        }
        if (message.contains("菜品") || message.contains("明细")) {
            return AiQueryIntent.ITEMS;
        }
        if (message.contains("详情")) {
            return AiQueryIntent.DETAIL;
        }
        return AiQueryIntent.DETAIL;
    }
}
