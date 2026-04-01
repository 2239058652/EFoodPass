package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.ai.dto.*;
import com.epass.food.modules.ai.service.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
        PromptBuildResult promptBuildResult = buildPromptByScene(sceneType, message, currentUserId, canViewAnyOrder);

        String rawContent = chatClient.prompt()
                .system(promptBuildResult.getPrompt())
                .user(message)
                .call()
                .content();

        AiStructuredReply reply = parseStructuredReply(rawContent);
        String nextAction = resolveNextAction(message, sceneType, promptBuildResult.getAnswerType());

        return new AiChatResponse(
                reply.getContent(),
                sceneType.name().toLowerCase(),
                promptBuildResult.isGrounded(),
                nextAction,
                promptBuildResult.getAnswerType().name().toLowerCase()
        );
    }

    private AiStructuredReply parseStructuredReply(String rawContent) {
        try {
            return objectMapper.readValue(rawContent, AiStructuredReply.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "AI 返回结果不是合法 JSON: " + rawContent);
        }
    }

    private PromptBuildResult buildPromptByScene(AiSceneType sceneType,
                                                 String message,
                                                 Long currentUserId,
                                                 boolean canViewAnyOrder) {
        return switch (sceneType) {
            case ORDER -> buildOrderPrompt(message, currentUserId, canViewAnyOrder);
            case ITEM -> new PromptBuildResult(buildItemPrompt(), AiAnswerType.NORMAL, true);
            case STOCK -> new PromptBuildResult(buildStockPrompt(), AiAnswerType.NORMAL, true);
            case SYSTEM -> new PromptBuildResult(buildSystemPrompt(), AiAnswerType.NORMAL, true);
            case GENERAL -> new PromptBuildResult(buildGeneralPrompt(), AiAnswerType.NORMAL, true);
        };
    }

    private String resolveNextAction(String message, AiSceneType sceneType, AiAnswerType answerType) {
        if (answerType == AiAnswerType.RESTRICTED || answerType == AiAnswerType.NOT_FOUND) {
            return "ask_more_details";
        }

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
            case DETAIL_QUERY -> "ask_more_details";
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
                }
                """.formatted(businessContextProvider.buildGeneralAssistantPrompt());
    }

    private PromptBuildResult buildOrderPrompt(String message, Long currentUserId, boolean canViewAnyOrder) {
        OrderQuestionType questionType = orderQuestionClassifier.classify(message);

        return switch (questionType) {
            case DETAIL_QUERY -> buildOrderDetailPrompt(message, currentUserId, canViewAnyOrder);
            case STATUS_RULE -> new PromptBuildResult(buildOrderStatusPrompt(), AiAnswerType.NORMAL, true);
            case REALTIME_STATS -> new PromptBuildResult(buildOrderRealtimePrompt(), AiAnswerType.NORMAL, true);
            case GENERAL_ORDER -> new PromptBuildResult(buildOrderGeneralPrompt(), AiAnswerType.NORMAL, true);
        };
    }

    private PromptBuildResult buildOrderDetailPrompt(String message, Long currentUserId, boolean canViewAnyOrder) {
        Long orderId = orderIdExtractor.extractOrderId(message);
        if (orderId == null) {
            return new PromptBuildResult(buildOrderGeneralPrompt(), AiAnswerType.NORMAL, true);
        }

        try {
            String detailFacts = orderAiSupportService.buildOrderDetailFacts(currentUserId, canViewAnyOrder, orderId);

            String prompt = """
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
                    }
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    orderFactProvider.buildOrderFacts(),
                    detailFacts
            );

            return new PromptBuildResult(prompt, AiAnswerType.NORMAL, true);
        } catch (BusinessException e) {
            AiAnswerType answerType = resolveAnswerType(e);

            String prompt = """
                    %s
                    
                    下面是订单领域的静态业务事实：
                    %s
                    
                    当前有一条真实业务限制信息：
                    - 无法加载该订单详情，原因：%s
                    
                    你现在是 EFoodPass 的订单助手。
                    用户正在查询某个订单详情，但当前系统无法返回该订单的具体内容。
                    请基于这条真实限制信息，用简洁中文说明情况，不要编造任何订单详情。
                    
                    你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                    JSON 格式如下：
                    {
                      "content": "给用户的中文回答",
                    }
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    orderFactProvider.buildOrderFacts(),
                    buildOrderAccessHint(answerType)
            );

            return new PromptBuildResult(prompt, answerType, true);
        }
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
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts()
        );
    }

    private AiAnswerType resolveAnswerType(BusinessException e) {
        Integer code = e.getCode();
        if (code == null) {
            return AiAnswerType.RESTRICTED;
        }

        return switch (code) {
            case BizErrorCode.ORDER_NOT_FOUND -> AiAnswerType.NOT_FOUND;
            case BizErrorCode.ORDER_NO_PERMISSION -> AiAnswerType.RESTRICTED;
            default -> AiAnswerType.RESTRICTED;
        };
    }

    private String buildOrderAccessHint(AiAnswerType answerType) {
        return switch (answerType) {
            case NOT_FOUND -> "该订单不存在";
            case RESTRICTED -> "当前登录用户无权查看该订单";
            case NORMAL -> "订单详情可正常访问";
        };
    }

    @Getter
    private static class PromptBuildResult {
        private final String prompt;
        private final AiAnswerType answerType;
        private final boolean grounded;

        private PromptBuildResult(String prompt, AiAnswerType answerType, boolean grounded) {
            this.prompt = prompt;
            this.answerType = answerType;
            this.grounded = grounded;
        }

    }
}