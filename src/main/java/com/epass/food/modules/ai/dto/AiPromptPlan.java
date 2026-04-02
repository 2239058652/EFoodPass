package com.epass.food.modules.ai.dto;

import org.springframework.ai.chat.client.advisor.api.Advisor;

import java.util.Map;

public record AiPromptPlan(
        String prompt,
        AiAnswerType answerType,
        boolean grounded,
        String nextAction,
        AiDisplayCard card,
        Object[] tools,
        Map<String, Object> toolContext,
        Map<String, Object> advisorParams,
        Advisor[] advisors
) {

    public AiPromptPlan(String prompt,
                        AiAnswerType answerType,
                        boolean grounded,
                        String nextAction,
                        AiDisplayCard card) {
        this(prompt, answerType, grounded, nextAction, card, new Object[0], Map.of(), Map.of(), new Advisor[0]);
    }

    public AiPromptPlan(String prompt,
                        AiAnswerType answerType,
                        boolean grounded,
                        String nextAction,
                        AiDisplayCard card,
                        Object[] tools,
                        Map<String, Object> toolContext) {
        this(prompt, answerType, grounded, nextAction, card, tools, toolContext, Map.of(), new Advisor[0]);
    }

    public AiPromptPlan(String prompt,
                        AiAnswerType answerType,
                        boolean grounded,
                        String nextAction,
                        AiDisplayCard card,
                        Object[] tools,
                        Map<String, Object> toolContext,
                        Map<String, Object> advisorParams) {
        this(prompt, answerType, grounded, nextAction, card, tools, toolContext, advisorParams, new Advisor[0]);
    }

    public boolean hasTools() {
        return tools != null && tools.length > 0;
    }

    public boolean hasAdvisors() {
        return advisors != null && advisors.length > 0;
    }
}
