package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.dto.AiStructuredReply;
import com.epass.food.modules.ai.dto.OrderQuestionType;
import com.epass.food.modules.ai.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final BusinessContextProvider businessContextProvider;
    private final OrderFactProvider orderFactProvider;
    private final AiSceneClassifier aiSceneClassifier;
    private final ObjectMapper objectMapper;

    private final ItemFactProvider itemFactProvider;
    private final StockFactProvider stockFactProvider;
    private final SystemFactProvider systemFactProvider;

    private final OrderAiSupportService orderAiSupportService;
    private final OrderQuestionClassifier orderQuestionClassifier;
    private final OrderIdExtractor orderIdExtractor;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             BusinessContextProvider businessContextProvider,
                             OrderFactProvider orderFactProvider,
                             ItemFactProvider itemFactProvider,
                             StockFactProvider stockFactProvider,
                             SystemFactProvider systemFactProvider,
                             OrderAiSupportService orderAiSupportService,
                             AiSceneClassifier aiSceneClassifier,
                             ObjectMapper objectMapper,
                             OrderQuestionClassifier orderQuestionClassifier,
                             OrderIdExtractor orderIdExtractor) {
        this.chatClient = chatClientBuilder.build();
        this.businessContextProvider = businessContextProvider;
        this.orderFactProvider = orderFactProvider;
        this.itemFactProvider = itemFactProvider;
        this.stockFactProvider = stockFactProvider;
        this.systemFactProvider = systemFactProvider;
        this.orderAiSupportService = orderAiSupportService;
        this.aiSceneClassifier = aiSceneClassifier;
        this.objectMapper = objectMapper;
        this.orderQuestionClassifier = orderQuestionClassifier;
        this.orderIdExtractor = orderIdExtractor;
    }

    @Override
    public AiChatResponse chat(String message, Long currentUserId, boolean canViewAnyOrder) {
        AiSceneType sceneType = aiSceneClassifier.classify(message);
        String systemPrompt = buildPromptByScene(sceneType, message, currentUserId, canViewAnyOrder);

        String rawContent = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();

        AiStructuredReply reply = parseStructuredReply(rawContent);
        String nextAction = resolveNextAction(message, sceneType);

        return new AiChatResponse(
                reply.getContent(),
                reply.getScene(),
                reply.getGrounded(),
                nextAction
        );
    }

    private AiStructuredReply parseStructuredReply(String rawContent) {
        try {
            return objectMapper.readValue(rawContent, AiStructuredReply.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "AI 返回结果不是合法 JSON: " + rawContent);
        }
    }

    private String buildPromptByScene(AiSceneType sceneType,
                                      String message,
                                      Long currentUserId,
                                      boolean canViewAnyOrder) {
        return switch (sceneType) {
            case ORDER -> buildOrderPrompt(message, currentUserId, canViewAnyOrder);
            case ITEM -> buildItemPrompt();
            case STOCK -> buildStockPrompt();
            case SYSTEM -> buildSystemPrompt();
            case GENERAL -> buildGeneralPrompt();
        };
    }

    private String resolveNextAction(String message, AiSceneType sceneType) {
        return switch (sceneType) {
            case ORDER -> resolveOrderAction(message);
            case ITEM -> "view_item_module";
            case STOCK -> "view_stock_module";
            case SYSTEM -> "view_system_module";
            case GENERAL -> resolveGeneralAction(message);
        };
    }

    private String resolveOrderAction(String message) {
        OrderQuestionType questionType = orderQuestionClassifier.classify(message);

        return switch (questionType) {
            case DETAIL_QUERY -> "view_order_detail";
            case STATUS_RULE -> "view_order_status";
            case REALTIME_STATS, GENERAL_ORDER -> "view_order_module";
        };
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
                  "content": "给用户的中文回答",
                  "scene": "general",
                  "grounded": true
                }
                """.formatted(businessContextProvider.buildGeneralAssistantPrompt());
    }

    private String buildOrderPrompt(String message, Long currentUserId, boolean canViewAnyOrder) {
        OrderQuestionType questionType = orderQuestionClassifier.classify(message);

        return switch (questionType) {
            case DETAIL_QUERY -> buildOrderDetailPrompt(message, currentUserId, canViewAnyOrder);
            case STATUS_RULE -> buildOrderStatusPrompt();
            case REALTIME_STATS -> buildOrderRealtimePrompt();
            case GENERAL_ORDER -> buildOrderGeneralPrompt();
        };
    }

    private String buildOrderDetailPrompt(String message, Long currentUserId, boolean canViewAnyOrder) {
        Long orderId = orderIdExtractor.extractOrderId(message);
        if (orderId == null) {
            return buildOrderGeneralPrompt();
        }

        return """
                %s
                
                下面是订单领域的静态业务事实：
                %s
                
                下面是当前用户有权访问的指定订单真实详情：
                %s
                
                你现在是 EFoodPass 的订单助手。
                当前问题是在询问指定订单的详情，请严格基于这些真实事实回答。
                如果事实里没有，不要编造。
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答",
                  "scene": "order",
                  "grounded": true
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts(),
                orderAiSupportService.buildOrderDetailFacts(currentUserId, canViewAnyOrder, orderId)
        );
    }

    private String buildItemPrompt() {
        return """
                %s
                
                下面是菜品领域的真实业务事实：
                %s
                
                你现在是 EFoodPass 的菜品助手。
                请严格基于这些真实事实回答菜品问题。
                如果事实里没有，不要编造。
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答",
                  "scene": "item",
                  "grounded": true
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                itemFactProvider.buildItemFacts()
        );
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
                  "content": "给用户的中文回答",
                  "scene": "stock",
                  "grounded": true
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
                  "content": "给用户的中文回答",
                  "scene": "system",
                  "grounded": true
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                systemFactProvider.buildSystemFacts()
        );
    }

    //    订单状态/规则类问题，不查实时数据
    private String buildOrderStatusPrompt() {
        return """
                %s
                
                下面是订单领域的静态业务事实：
                %s
                
                你现在是 EFoodPass 的订单助手。
                当前问题更偏向订单规则、订单状态定义或订单流程说明。
                请严格基于这些真实事实回答，不要编造。
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答",
                  "scene": "order",
                  "grounded": true
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts()
        );
    }

    //    实时统计类问题，加载动态数据
    private String buildOrderRealtimePrompt() {
        return """
                %s
                
                下面是订单领域的静态业务事实：
                %s
                
                下面是订单领域的实时业务数据：
                %s
                
                你现在是 EFoodPass 的订单助手。
                当前问题更偏向订单统计、数量、金额或实时情况。
                请严格基于这些真实事实和实时数据回答，不要编造。
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答",
                  "scene": "order",
                  "grounded": true
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts(),
                orderAiSupportService.buildRealtimeOrderFacts()
        );
    }

    //    一般订单问题，只保留静态事实
    private String buildOrderGeneralPrompt() {
        return """
                %s
                
                下面是订单领域的静态业务事实：
                %s
                
                你现在是 EFoodPass 的订单助手。
                当前问题属于一般订单问题。
                请严格基于这些真实事实回答，不要编造。
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答",
                  "scene": "order",
                  "grounded": true
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts()
        );
    }
}