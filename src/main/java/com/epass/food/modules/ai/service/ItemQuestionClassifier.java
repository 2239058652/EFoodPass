package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.ItemQuestionType;
import org.springframework.stereotype.Component;

@Component
public class ItemQuestionClassifier {

    private final ItemEntityReferenceResolver itemEntityReferenceResolver;

    public ItemQuestionClassifier(ItemEntityReferenceResolver itemEntityReferenceResolver) {
        this.itemEntityReferenceResolver = itemEntityReferenceResolver;
    }

    public ItemQuestionType classify(String message) {
        if (message == null || message.isBlank()) {
            return ItemQuestionType.GENERAL_ITEM;
        }

        if (itemEntityReferenceResolver.resolve(message) != null) {
            return ItemQuestionType.DETAIL_QUERY;
        }

        if (message.contains("上架状态")
                || message.contains("下架状态")
                || message.contains("菜品状态")) {
            return ItemQuestionType.STATUS_RULE;
        }

        return ItemQuestionType.GENERAL_ITEM;
    }
}