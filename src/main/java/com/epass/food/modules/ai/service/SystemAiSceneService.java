package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiDisplayField;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;
    private final SystemModuleCatalog systemModuleCatalog;
    private final QuestionAnswerAdvisor systemKnowledgeAdvisor;
    private final SystemKnowledgeRagProperties ragProperties;

    public SystemAiSceneService(BusinessContextProvider businessContextProvider,
                                SystemModuleCatalog systemModuleCatalog,
                                QuestionAnswerAdvisor systemKnowledgeAdvisor,
                                SystemKnowledgeRagProperties ragProperties) {
        this.businessContextProvider = businessContextProvider;
        this.systemModuleCatalog = systemModuleCatalog;
        this.systemKnowledgeAdvisor = systemKnowledgeAdvisor;
        this.ragProperties = ragProperties;
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
                buildCard(context.message()),
                new Object[0],
                Map.of(),
                buildAdvisorParams(context.message()),
                new Advisor[]{systemKnowledgeAdvisor}
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

    private AiDisplayCard buildCard(String message) {
        String filterExpression = resolveFilterExpression(message);
        String retrievalScope = filterExpression == null ? "全量系统知识库" : "过滤检索: " + filterExpression;

        return new AiDisplayCard(
                "系统模块",
                "system-modules",
                "当前卡片展示系统核心模块清单，本场景回答会先检索项目知识库。",
                List.of(
                        new AiDisplayField("知识库", "system"),
                        new AiDisplayField("检索范围", retrievalScope),
                        new AiDisplayField("TopK", String.valueOf(ragProperties.getTopK())),
                        new AiDisplayField("相似度阈值", String.valueOf(ragProperties.getSimilarityThreshold()))
                )
        );
    }

    private Map<String, Object> buildAdvisorParams(String message) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(AiAdvisorContextKeys.STRUCTURED_FIELDS, List.of("content"));
        params.put(AiAdvisorContextKeys.RAG_KNOWLEDGE_BASE, "system");
        params.put(AiAdvisorContextKeys.RAG_TOP_K, ragProperties.getTopK());
        params.put(AiAdvisorContextKeys.RAG_SIMILARITY_THRESHOLD, ragProperties.getSimilarityThreshold());

        String filterExpression = resolveFilterExpression(message);
        if (filterExpression != null) {
            params.put(QuestionAnswerAdvisor.FILTER_EXPRESSION, filterExpression);
            params.put(AiAdvisorContextKeys.RAG_FILTER_EXPRESSION, filterExpression);
        }
        return params;
    }

    private String resolveFilterExpression(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }

        String normalized = message.toLowerCase();
        if (normalized.contains("auth") || message.contains("登录") || message.contains("当前用户") || normalized.contains("token")) {
            return "moduleCode == 'auth'";
        }
        if (message.contains("权限") || message.contains("角色") || message.contains("用户")
                || normalized.contains("security") || normalized.contains("preauthorize")) {
            return "moduleCode == 'system'";
        }
        if (message.contains("分类")) {
            return "moduleCode == 'food/category'";
        }
        if (message.contains("菜品")) {
            return "moduleCode == 'food/item'";
        }
        if (message.contains("订单")) {
            return "moduleCode == 'food/order'";
        }
        if (message.contains("库存")) {
            return "moduleCode == 'food/stock'";
        }
        return null;
    }
}
