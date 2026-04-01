package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class OrderIdExtractor {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("订单\\s*(\\d+)");

    public Long extractOrderId(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = ORDER_ID_PATTERN.matcher(message);
        if (matcher.find()) {
            return Long.valueOf(matcher.group(1));
        }

        return null;
    }
}