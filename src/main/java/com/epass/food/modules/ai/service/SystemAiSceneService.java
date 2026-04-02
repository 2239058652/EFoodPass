package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiDisplayField;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SystemAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;
    private final SystemModuleCatalog systemModuleCatalog;
    private final QuestionAnswerAdvisor systemKnowledgeAdvisor;

    public SystemAiSceneService(BusinessContextProvider businessContextProvider,
                                SystemModuleCatalog systemModuleCatalog,
                                QuestionAnswerAdvisor systemKnowledgeAdvisor) {
        this.businessContextProvider = businessContextProvider;
        this.systemModuleCatalog = systemModuleCatalog;
        this.systemKnowledgeAdvisor = systemKnowledgeAdvisor;
    }

    @Override
    public AiSceneType sceneType() {
        return AiSceneType.SYSTEM;
    }

    @Override
    public AiPromptPlan buildPlan(AiSceneRequestContext context) {
        return new AiPromptPlan(
                buildPrompt(),
                AiAnswerType.NORMAL,
                true,
                "view_system_module",
                buildCard(),
                new Object[0],
                Map.of(),
                Map.of(AiAdvisorContextKeys.STRUCTURED_FIELDS, List.of("content")),
                new org.springframework.ai.chat.client.advisor.api.Advisor[]{systemKnowledgeAdvisor}
        );
    }

    private String buildPrompt() {
        return """
                %s

                你现在是 EFoodPass 的系统管理助手。
                当前问题主要围绕系统模块、认证登录、角色权限和鉴权机制。
                你应该优先依据检索到的项目知识片段回答。
                如果检索结果不足，请明确说明信息不足，不要编造不存在的模块、接口或权限规则。
                回答保持中文、简洁、面向项目实际实现。
                """.formatted(businessContextProvider.buildCommonFacts());
    }

    private AiDisplayCard buildCard() {
        return new AiDisplayCard(
                "系统模块",
                "system-modules",
                "当前卡片展示系统核心模块清单，本场景回答会先检索项目知识库。",
                systemModuleCatalog.getModules().stream()
                        .map(module -> new AiDisplayField(module.code(), module.description()))
                        .toList()
        );
    }
}
