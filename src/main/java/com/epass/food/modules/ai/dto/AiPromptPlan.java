package com.epass.food.modules.ai.dto;

import java.util.Map;

public record AiPromptPlan(
        String prompt,
        AiAnswerType answerType,
        boolean grounded,
        String nextAction,
        AiDisplayCard card,
        Object[] tools,
        Map<String, Object> toolContext
) {

    public AiPromptPlan(String prompt,
                        AiAnswerType answerType,
                        boolean grounded,
                        String nextAction,
                        AiDisplayCard card) {
        this(prompt, answerType, grounded, nextAction, card, new Object[0], Map.of());
    }

    public boolean hasTools() {
        return tools != null && tools.length > 0;
    }
}
