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
import com.epass.food.modules.ai.service.StockAiSceneService;
import com.epass.food.modules.ai.service.SystemAiSceneService;
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
    private final OrderAiSceneService orderAiSceneService;
    private final ItemAiSceneService itemAiSceneService;
    private final StockAiSceneService stockAiSceneService;
    private final SystemAiSceneService systemAiSceneService;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             BusinessContextProvider businessContextProvider,
                             AiSceneClassifier aiSceneClassifier,
                             ObjectMapper objectMapper,
                             OrderAiSceneService orderAiSceneService,
                             ItemAiSceneService itemAiSceneService,
                             StockAiSceneService stockAiSceneService,
                             SystemAiSceneService systemAiSceneService) {
        this.chatClient = chatClientBuilder.build();
        this.businessContextProvider = businessContextProvider;
        this.aiSceneClassifier = aiSceneClassifier;
        this.objectMapper = objectMapper;
        this.orderAiSceneService = orderAiSceneService;
        this.itemAiSceneService = itemAiSceneService;
        this.stockAiSceneService = stockAiSceneService;
        this.systemAiSceneService = systemAiSceneService;
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
            case STOCK -> stockAiSceneService.buildPlan();
            case SYSTEM -> systemAiSceneService.buildPlan();
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
}
