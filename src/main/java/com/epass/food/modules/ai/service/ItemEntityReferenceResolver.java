package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiEntityReference;
import com.epass.food.modules.ai.dto.AiQueryIntent;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ItemEntityReferenceResolver {

    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("(菜品|商品)\\s*(\\d+)");

    public AiEntityReference resolve(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        Matcher matcher = ITEM_ID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        Long itemId = Long.valueOf(matcher.group(2));
        return new AiEntityReference("item", itemId, resolveIntent(message));
    }

    private AiQueryIntent resolveIntent(String message) {
        if (message.contains("状态") || message.contains("上架") || message.contains("下架")) {
            return AiQueryIntent.STATUS;
        }
        if (message.contains("库存")) {
            return AiQueryIntent.STOCK;
        }
        if (message.contains("分类")) {
            return AiQueryIntent.CATEGORY;
        }
        if (message.contains("详情")) {
            return AiQueryIntent.DETAIL;
        }
        return AiQueryIntent.DETAIL;
    }
}
