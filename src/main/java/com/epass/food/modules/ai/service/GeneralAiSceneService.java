package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneType;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public AiPromptPlan buildPlan(String message, Long currentUserId, boolean canViewAnyOrder) {
        return new AiPromptPlan(
                """
                        %s

                        你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                        JSON 格式如下：
                        {
                          "content": "给用户的中文回答"
                        }
                        """.formatted(businessContextProvider.buildGeneralAssistantPrompt()),
                AiAnswerType.NORMAL,
                true,
                resolveAction(message),
                new AiDisplayCard("通用助手", "general", "已基于当前项目通用事实生成回答。", List.of())
        );
    }

    private String resolveAction(String message) {
        if (message != null && message.contains("模块")) {
            return "view_system_modules";
        }
        return "none";
    }
}
