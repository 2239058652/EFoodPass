package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.ai.dto.*;
import com.epass.food.modules.ai.service.*;
import com.epass.food.modules.food.item.enums.FoodItemSaleStatus;
import com.epass.food.modules.food.order.enums.FoodOrderStatus;
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
    private final SystemModuleCatalog systemModuleCatalog;
    private final StockChangeSceneCatalog stockChangeSceneCatalog;
    private final OrderEntityReferenceResolver orderEntityReferenceResolver;
    private final ItemQuestionClassifier itemQuestionClassifier;
    private final ItemEntityReferenceResolver itemEntityReferenceResolver;
    private final ItemAiSupportService itemAiSupportService;

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
                             OrderIdExtractor orderIdExtractor,
                             SystemModuleCatalog systemModuleCatalog,
                             StockChangeSceneCatalog stockChangeSceneCatalog,
                             OrderEntityReferenceResolver orderEntityReferenceResolver,
                             ItemQuestionClassifier itemQuestionClassifier,
                             ItemEntityReferenceResolver itemEntityReferenceResolver,
                             ItemAiSupportService itemAiSupportService) {
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
        this.systemModuleCatalog = systemModuleCatalog;
        this.stockChangeSceneCatalog = stockChangeSceneCatalog;
        this.orderEntityReferenceResolver = orderEntityReferenceResolver;
        this.itemQuestionClassifier = itemQuestionClassifier;
        this.itemEntityReferenceResolver = itemEntityReferenceResolver;
        this.itemAiSupportService = itemAiSupportService;
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

        AiDisplayCard card = buildDisplayCard(
                message,
                sceneType,
                promptBuildResult.getAnswerType(),
                reply.getContent(),
                currentUserId,
                canViewAnyOrder
        );

        return new AiChatResponse(
                reply.getContent(),
                sceneType.name().toLowerCase(),
                promptBuildResult.isGrounded(),
                nextAction,
                promptBuildResult.getAnswerType().name().toLowerCase(),
                card
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
            case ITEM -> buildItemPrompt(message);
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
            case ITEM -> resolveItemAction(message, answerType);
            case STOCK -> "view_stock_module";
            case SYSTEM -> "view_system_module";
            case GENERAL -> resolveGeneralAction(message);
        };
    }

    private String resolveItemAction(String message, AiAnswerType answerType) {
        if (answerType == AiAnswerType.NOT_FOUND) {
            return "ask_more_details";
        }

        ItemQuestionType questionType = itemQuestionClassifier.classify(message);
        return switch (questionType) {
            case DETAIL_QUERY -> "view_item_detail";
            case STATUS_RULE -> "view_item_status";
            case GENERAL_ITEM -> "view_item_module";
        };
    }

    private String resolveOrderAction(String message) {
        OrderQuestionType questionType = orderQuestionClassifier.classify(message);

        if (questionType == OrderQuestionType.DETAIL_QUERY) {
            var reference = orderEntityReferenceResolver.resolve(message);
            if (reference == null) {
                return "ask_more_details";
            }

            return switch (reference.getIntent()) {
                case STATUS -> "view_order_status";
                case ITEMS, DETAIL, AMOUNT -> "view_order_detail";
                default -> "view_order_module";
            };
        }

        return switch (questionType) {
            case STATUS_RULE -> "view_order_status";
            case REALTIME_STATS, GENERAL_ORDER -> "view_order_module";
            case DETAIL_QUERY -> "view_order_detail";
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
        var reference = orderEntityReferenceResolver.resolve(message);
        if (reference == null || reference.getEntityId() == null) {
            return new PromptBuildResult(buildOrderGeneralPrompt(), AiAnswerType.NORMAL, true);
        }

        try {
            String detailFacts = orderAiSupportService.buildOrderDetailFacts(
                    currentUserId,
                    canViewAnyOrder,
                    reference.getEntityId()
            );

            String intentHint = buildOrderIntentHint(reference);

            String prompt = """
                    %s
                    
                    下面是订单领域的静态业务事实：
                    %s
                    
                    下面是当前用户有权访问的指定订单真实详情：
                    %s
                    
                    当前用户问题的查询重点是：
                    %s
                    
                    你现在是 EFoodPass 的订单助手。
                    请优先围绕这个查询重点回答，不要展开无关内容，也不要编造。
                    
                    你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                    JSON 格式如下：
                    {
                      "content": "给用户的中文回答"
                    }
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    orderFactProvider.buildOrderFacts(),
                    detailFacts,
                    intentHint
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
                    请基于这条真实限制信息，用简洁中文说明情况，不要编造任何订单详情。
                    
                    你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                    JSON 格式如下：
                    {
                      "content": "给用户的中文回答"
                    }
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    orderFactProvider.buildOrderFacts(),
                    buildOrderAccessHint(answerType)
            );

            return new PromptBuildResult(prompt, answerType, true);
        }
    }

    private String buildOrderIntentHint(com.epass.food.modules.ai.dto.AiEntityReference reference) {
        return switch (reference.getIntent()) {
            case STATUS -> "用户重点想知道该订单当前状态";
            case AMOUNT -> "用户重点想知道该订单金额信息";
            case ITEMS -> "用户重点想知道该订单的菜品明细";
            case DETAIL -> "用户重点想知道该订单的完整详情";
            default -> "用户重点想了解该订单的基本信息";
        };
    }

    private PromptBuildResult buildItemPrompt(String message) {
        ItemQuestionType questionType = itemQuestionClassifier.classify(message);

        return switch (questionType) {
            case DETAIL_QUERY -> buildItemDetailPrompt(message);
            case STATUS_RULE -> new PromptBuildResult(buildItemStatusPrompt(), AiAnswerType.NORMAL, true);
            case GENERAL_ITEM -> new PromptBuildResult(buildItemGeneralPrompt(), AiAnswerType.NORMAL, true);
        };
    }

    private String buildItemStatusPrompt() {
        return """
                %s
                
                下面是菜品领域的真实业务事实：
                %s
                
                当前问题更偏向菜品状态规则说明。
                请严格基于这些真实事实回答，不要编造。
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                itemFactProvider.buildItemFacts()
        );
    }

    private String buildItemGeneralPrompt() {
        return """
                %s
                
                下面是菜品领域的真实业务事实：
                %s
                
                当前问题属于一般菜品问题。
                请严格基于这些真实事实回答，不要编造。
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                itemFactProvider.buildItemFacts()
        );
    }

    private PromptBuildResult buildItemDetailPrompt(String message) {
        var reference = itemEntityReferenceResolver.resolve(message);
        if (reference == null || reference.getEntityId() == null) {
            return new PromptBuildResult(buildItemGeneralPrompt(), AiAnswerType.NORMAL, true);
        }

        try {
            String detailFacts = itemAiSupportService.buildItemDetailFacts(reference.getEntityId());

            String prompt = """
                    %s
                    
                    下面是菜品领域的真实业务事实：
                    %s
                    
                    下面是指定菜品的真实详情：
                    %s
                    
                    当前用户问题的查询重点是：
                    %s
                    
                    你现在是 EFoodPass 的菜品助手。
                    请优先围绕这个查询重点回答，不要展开无关内容，也不要编造。
                    
                    你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                    JSON 格式如下：
                    {
                      "content": "给用户的中文回答"
                    }
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    itemFactProvider.buildItemFacts(),
                    detailFacts,
                    buildItemIntentHint(reference)
            );

            return new PromptBuildResult(prompt, AiAnswerType.NORMAL, true);
        } catch (BusinessException e) {
            String prompt = """
                    %s
                    
                    下面是菜品领域的真实业务事实：
                    %s
                    
                    当前有一条真实业务限制信息：
                    - 无法加载该菜品详情，原因：菜品不存在
                    
                    你现在是 EFoodPass 的菜品助手。
                    请基于这条真实限制信息，用简洁中文说明情况，不要编造任何菜品详情。
                    
                    你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                    JSON 格式如下：
                    {
                      "content": "给用户的中文回答"
                    }
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    itemFactProvider.buildItemFacts()
            );

            return new PromptBuildResult(prompt, AiAnswerType.NOT_FOUND, true);
        }
    }

    private String buildItemIntentHint(AiEntityReference reference) {
        return switch (reference.getIntent()) {
            case STATUS -> "用户重点想知道该菜品当前上架状态";
            case STOCK -> "用户重点想知道该菜品当前库存";
            case CATEGORY -> "用户重点想知道该菜品所属分类";
            case DETAIL -> "用户重点想知道该菜品完整详情";
            default -> "用户重点想了解该菜品的基本信息";
        };
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

    private AiDisplayCard buildDisplayCard(String message,
                                           AiSceneType sceneType,
                                           AiAnswerType answerType,
                                           String content,
                                           Long currentUserId,
                                           boolean canViewAnyOrder) {
        if (answerType == AiAnswerType.NOT_FOUND) {
            return new AiDisplayCard(
                    "未找到目标数据",
                    "not-found",
                    "当前查询目标不存在，请确认输入的信息是否正确。", java.util.List.of()
            );
        }

        if (answerType == AiAnswerType.RESTRICTED) {
            return new AiDisplayCard(
                    "访问受限",
                    "restricted",
                    "当前登录用户无权访问该数据，或系统不允许返回详情。", java.util.List.of()
            );
        }

        return switch (sceneType) {
            case ORDER -> buildOrderCard(message, currentUserId, canViewAnyOrder);
            case ITEM -> buildItemCard(message, answerType);
            case STOCK -> buildStockCard();
            case SYSTEM -> buildSystemCard();
            case GENERAL ->
                    new AiDisplayCard("通用助手", "general", "已基于当前项目通用事实生成回答。", java.util.List.of());
        };
    }

    private AiDisplayCard buildItemCard(String message, AiAnswerType answerType) {
        if (answerType == AiAnswerType.NOT_FOUND) {
            return new AiDisplayCard(
                    "未找到菜品",
                    "item-not-found",
                    "当前查询的菜品不存在，请确认菜品编号。",
                    java.util.List.of()
            );
        }

        ItemQuestionType questionType = itemQuestionClassifier.classify(message);
        return switch (questionType) {
            case DETAIL_QUERY -> buildItemDetailCard(message);
            case STATUS_RULE -> new AiDisplayCard(
                    "菜品状态说明",
                    "item-status",
                    "当前卡片展示菜品上下架状态。",
                    buildItemStatusFields()
            );
            case GENERAL_ITEM -> new AiDisplayCard(
                    "菜品助手",
                    "item",
                    "当前回答围绕菜品领域的一般问题生成。",
                    java.util.List.of()
            );
        };
    }

    private AiDisplayCard buildItemDetailCard(String message) {
        var reference = itemEntityReferenceResolver.resolve(message);
        if (reference == null || reference.getEntityId() == null) {
            return new AiDisplayCard(
                    "菜品详情",
                    "item-detail",
                    "未能从问题中提取菜品编号。",
                    java.util.List.of()
            );
        }

        try {
            var item = itemAiSupportService.getItemDetail(reference.getEntityId());

            return new AiDisplayCard(
                    "菜品详情",
                    "item-detail",
                    "当前卡片展示指定菜品的关键字段。",
                    java.util.List.of(
                            new AiDisplayField("菜品ID", String.valueOf(item.getId())),
                            new AiDisplayField("菜品名称", item.getName()),
                            new AiDisplayField("分类ID", String.valueOf(item.getCategoryId())),
                            new AiDisplayField("当前价格", String.valueOf(item.getPrice())),
                            new AiDisplayField("当前库存", String.valueOf(item.getStock())),
                            new AiDisplayField(
                                    "上架状态",
                                    item.getIsOnSale() + "（" +
                                            com.epass.food.modules.food.item.enums.FoodItemSaleStatus.getLabelByCode(item.getIsOnSale()) +
                                            "）"
                            )
                    )
            );
        } catch (BusinessException e) {
            return new AiDisplayCard(
                    "菜品详情",
                    "item-detail",
                    "当前无法展示菜品关键字段。",
                    java.util.List.of()
            );
        }
    }

    private AiDisplayCard buildOrderCard(String message, Long currentUserId, boolean canViewAnyOrder) {
        OrderQuestionType questionType = orderQuestionClassifier.classify(message);

        return switch (questionType) {
            case DETAIL_QUERY -> buildOrderDetailCard(message, currentUserId, canViewAnyOrder);
            case STATUS_RULE -> new AiDisplayCard(
                    "订单状态说明",
                    "order-status",
                    "当前回答围绕订单状态定义与规则生成。",
                    buildOrderStatusFields()
            );
            case REALTIME_STATS -> buildOrderStatsCard();
            case GENERAL_ORDER -> new AiDisplayCard(
                    "订单助手",
                    "order",
                    "当前回答围绕订单领域的一般问题生成。",
                    java.util.List.of()
            );
        };
    }

    private java.util.List<AiDisplayField> buildOrderStatusFields() {
        return java.util.Arrays.stream(FoodOrderStatus.values())
                .map(status -> new AiDisplayField(
                        "状态 " + status.getCode(),
                        status.getLabel()
                ))
                .toList();
    }

    private AiDisplayCard buildOrderDetailCard(String message) {
        Long orderId = orderIdExtractor.extractOrderId(message);
        if (orderId == null) {
            return new AiDisplayCard(
                    "订单详情",
                    "order-detail",
                    "未能从问题中提取订单编号。",
                    java.util.List.of()
            );
        }

        return new AiDisplayCard(
                "订单详情",
                "order-detail",
                "当前问题涉及指定订单详情。",
                java.util.List.of(
                        new AiDisplayField("订单ID", String.valueOf(orderId))
                )
        );
    }

    private AiDisplayCard buildOrderDetailCard(String message, Long currentUserId, boolean canViewAnyOrder) {
        Long orderId = orderIdExtractor.extractOrderId(message);
        if (orderId == null) {
            return new AiDisplayCard(
                    "订单详情",
                    "order-detail",
                    "未能从问题中提取订单编号。",
                    java.util.List.of()
            );
        }

        try {
            var detail = orderAiSupportService.getAccessibleOrderDetail(currentUserId, canViewAnyOrder, orderId);

            return new AiDisplayCard(
                    "订单详情",
                    "order-detail",
                    "当前卡片展示指定订单的关键字段。",
                    java.util.List.of(
                            new AiDisplayField("订单ID", String.valueOf(detail.getId())),
                            new AiDisplayField("订单编号", detail.getOrderNo()),
                            new AiDisplayField("用户ID", String.valueOf(detail.getUserId())),
                            new AiDisplayField("订单状态", detail.getOrderStatus() + "（" + FoodOrderStatus.getLabelByCode(detail.getOrderStatus()) + "）"),
                            new AiDisplayField("总金额", String.valueOf(detail.getTotalAmount())),
                            new AiDisplayField("下单时间", String.valueOf(detail.getCreatedAt()))
                    )
            );
        } catch (BusinessException e) {
            return new AiDisplayCard(
                    "订单详情",
                    "order-detail",
                    "当前无法展示订单关键字段。",
                    java.util.List.of()
            );
        }
    }

    private AiDisplayCard buildOrderStatsCard() {
        var overview = orderAiSupportService.getOrderStatOverview();

        return new AiDisplayCard(
                "订单实时统计",
                "order-stats",
                "当前卡片展示订单统计关键指标。",
                java.util.List.of(
                        new AiDisplayField("订单总数", String.valueOf(overview.getTotalOrderCount())),
                        new AiDisplayField("待确认", String.valueOf(overview.getPendingOrderCount())),
                        new AiDisplayField("制作中", String.valueOf(overview.getProcessingOrderCount())),
                        new AiDisplayField("已完成", String.valueOf(overview.getCompletedOrderCount())),
                        new AiDisplayField("已取消", String.valueOf(overview.getCanceledOrderCount())),
                        new AiDisplayField("订单总金额", String.valueOf(overview.getTotalAmount())),
                        new AiDisplayField("已完成金额", String.valueOf(overview.getCompletedAmount()))
                )
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

    private java.util.List<AiDisplayField> buildItemStatusFields() {
        return java.util.Arrays.stream(FoodItemSaleStatus.values())
                .map(status -> new AiDisplayField("状态 " + status.getCode(), status.getLabel()))
                .toList();
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