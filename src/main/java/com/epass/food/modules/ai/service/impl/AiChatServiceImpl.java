package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiDisplayField;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.dto.AiStructuredReply;
import com.epass.food.modules.ai.service.AiChatService;
import com.epass.food.modules.ai.service.AiSceneClassifier;
import com.epass.food.modules.ai.service.BusinessContextProvider;
import com.epass.food.modules.ai.service.ItemAiSceneService;
import com.epass.food.modules.ai.service.OrderAiSceneService;
import com.epass.food.modules.ai.service.StockChangeSceneCatalog;
import com.epass.food.modules.ai.service.StockFactProvider;
import com.epass.food.modules.ai.service.SystemFactProvider;
import com.epass.food.modules.ai.service.SystemModuleCatalog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final BusinessContextProvider businessContextProvider;
    private final AiSceneClassifier aiSceneClassifier;
    private final ObjectMapper objectMapper;
    private final StockFactProvider stockFactProvider;
    private final SystemFactProvider systemFactProvider;
    private final SystemModuleCatalog systemModuleCatalog;
    private final StockChangeSceneCatalog stockChangeSceneCatalog;
    private final OrderAiSceneService orderAiSceneService;
    private final ItemAiSceneService itemAiSceneService;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             BusinessContextProvider businessContextProvider,
                             AiSceneClassifier aiSceneClassifier,
                             ObjectMapper objectMapper,
                             StockFactProvider stockFactProvider,
                             SystemFactProvider systemFactProvider,
                             SystemModuleCatalog systemModuleCatalog,
                             StockChangeSceneCatalog stockChangeSceneCatalog,
                             OrderAiSceneService orderAiSceneService,
                             ItemAiSceneService itemAiSceneService) {
        this.chatClient = chatClientBuilder.build();
        this.businessContextProvider = businessContextProvider;
        this.aiSceneClassifier = aiSceneClassifier;
        this.objectMapper = objectMapper;
        this.stockFactProvider = stockFactProvider;
        this.systemFactProvider = systemFactProvider;
        this.systemModuleCatalog = systemModuleCatalog;
        this.stockChangeSceneCatalog = stockChangeSceneCatalog;
        this.orderAiSceneService = orderAiSceneService;
        this.itemAiSceneService = itemAiSceneService;
    }

    @Override
    public AiChatResponse chat(String message, Long currentUserId, boolean canViewAnyOrder) {
        AiSceneType sceneType = aiSceneClassifier.classify(message);
        AiPromptPlan promptPlan = buildPromptByScene(sceneType, message, currentUserId, canViewAnyOrder);

        String rawContent = chatClient.prompt()
                .system(promptPlan.prompt())
                .user(message)
                .call()
                .content();

        AiStructuredReply reply = parseStructuredReply(rawContent);
        return new AiChatResponse(
                reply.getContent(),
                sceneType.name().toLowerCase(),
                promptPlan.grounded(),
                promptPlan.nextAction(),
                promptPlan.answerType().name().toLowerCase(),
                promptPlan.card()
        );
    }

    private AiPromptPlan buildPromptByScene(AiSceneType sceneType,
                                            String message,
                                            Long currentUserId,
                                            boolean canViewAnyOrder) {
        return switch (sceneType) {
            case ORDER -> orderAiSceneService.buildPlan(message, currentUserId, canViewAnyOrder);
            case ITEM -> itemAiSceneService.buildPlan(message);
            case STOCK -> new AiPromptPlan(
                    buildStockPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_stock_module",
                    buildStockCard()
            );
            case SYSTEM -> new AiPromptPlan(
                    buildSystemPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_system_module",
                    buildSystemCard()
            );
            case GENERAL -> new AiPromptPlan(
                    buildGeneralPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    resolveGeneralAction(message),
                    new AiDisplayCard("通用助手", "general", "已基于当前项目通用事实生成回答。", List.of())
            );
        };
    }

    private AiStructuredReply parseStructuredReply(String rawContent) {
        try {
            return objectMapper.readValue(rawContent, AiStructuredReply.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "AI 返回结果不是合法 JSON: " + rawContent);
        }
    }

    private String resolveGeneralAction(String message) {
        if (message != null && message.contains("模块")) {
            return "view_system_modules";
        }
        return "none";
    }

    private String buildGeneralPrompt() {
        return """
                %s

                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(businessContextProvider.buildGeneralAssistantPrompt());
    }

    private String buildStockPrompt() {
        return """
                %s

                下面是库存领域的真实业务事实：
                %s

                你现在是 EFoodPass 的库存助手。
                请严格基于这些真实事实回答库存问题。
                如果事实里没有，不要编造。

                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                stockFactProvider.buildStockFacts()
        );
    }

    private String buildSystemPrompt() {
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

    private AiDisplayCard buildSystemCard() {
        return new AiDisplayCard(
                "系统模块",
                "system-modules",
                "当前卡片展示系统核心模块清单。",
                systemModuleCatalog.getModules().stream()
                        .map(module -> new AiDisplayField(module.code(), module.description()))
                        .toList()
        );
    }

    private AiDisplayCard buildStockCard() {
        return new AiDisplayCard(
                "库存日志场景",
                "stock-scenes",
                "当前卡片展示库存日志覆盖的变更场景。",
                stockChangeSceneCatalog.getScenes().stream()
                        .map(scene -> new AiDisplayField(scene.code(), scene.label()))
                        .toList()
        );
    }
}
