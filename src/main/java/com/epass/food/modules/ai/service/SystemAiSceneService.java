package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiDisplayField;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import org.springframework.stereotype.Service;

@Service
public class SystemAiSceneService {

    private final BusinessContextProvider businessContextProvider;
    private final SystemFactProvider systemFactProvider;
    private final SystemModuleCatalog systemModuleCatalog;

    public SystemAiSceneService(BusinessContextProvider businessContextProvider,
                                SystemFactProvider systemFactProvider,
                                SystemModuleCatalog systemModuleCatalog) {
        this.businessContextProvider = businessContextProvider;
        this.systemFactProvider = systemFactProvider;
        this.systemModuleCatalog = systemModuleCatalog;
    }

    public AiPromptPlan buildPlan() {
        return new AiPromptPlan(
                buildPrompt(),
                AiAnswerType.NORMAL,
                true,
                "view_system_module",
                buildCard()
        );
    }

    private String buildPrompt() {
        return """
                %s

                下面是系统管理领域的真实业务事实：
                %s

                你现在是 EFoodPass 的系统管理助手。
                请严格基于这些真实事实回答系统管理问题。
                如果事实里没有，不要编造。

                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                systemFactProvider.buildSystemFacts()
        );
    }

    private AiDisplayCard buildCard() {
        return new AiDisplayCard(
                "系统模块",
                "system-modules",
                "当前卡片展示系统核心模块清单。",
                systemModuleCatalog.getModules().stream()
                        .map(module -> new AiDisplayField(module.code(), module.description()))
                        .toList()
        );
    }
}
