package com.epass.food.modules.ai.dto;

public record AiPromptPlan(
        String prompt,
        AiAnswerType answerType,
        boolean grounded,
        String nextAction,
        AiDisplayCard card
) {
}
