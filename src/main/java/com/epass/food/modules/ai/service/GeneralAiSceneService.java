package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GeneralAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;

    public GeneralAiSceneService(BusinessContextProvider businessContextProvider) {
        this.businessContextProvider = businessContextProvider;
    }

    @Override
    public AiSceneType sceneType() {
        return AiSceneType.GENERAL;
    }

    @Override
    public AiPromptPlan buildPlan(AiSceneRequestContext context) {
        return new AiPromptPlan(
                """
                        %s

                        你现在是 EFoodPass 的通用助手。
                        请优先基于当前项目的真实业务事实回答问题。
                        如果问题超出已知项目信息，请明确说明，不要编造。
                        """.formatted(businessContextProvider.buildGeneralAssistantPrompt()),
                AiAnswerType.NORMAL,
                true,
                resolveAction(context.message()),
                new AiDisplayCard("通用助手", "general", "已基于当前项目通用事实生成回答。", List.of()),
                new Object[0],
                Map.of(),
                Map.of(AiAdvisorContextKeys.STRUCTURED_FIELDS, List.of("content"))
        );
    }

    private String resolveAction(String message) {
        if (message != null && message.contains("模块")) {
            return "view_system_modules";
        }
        return "none";
    }
}
