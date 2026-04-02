package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiDisplayField;
import com.epass.food.modules.ai.dto.AiEntityReference;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.dto.OrderQuestionType;
import com.epass.food.modules.food.order.enums.FoodOrderStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class OrderAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;
    private final OrderFactProvider orderFactProvider;
    private final OrderQuestionClassifier orderQuestionClassifier;
    private final OrderEntityReferenceResolver orderEntityReferenceResolver;
    private final OrderAiTools orderAiTools;

    public OrderAiSceneService(BusinessContextProvider businessContextProvider,
                               OrderFactProvider orderFactProvider,
                               OrderQuestionClassifier orderQuestionClassifier,
                               OrderEntityReferenceResolver orderEntityReferenceResolver,
                               OrderAiTools orderAiTools) {
        this.businessContextProvider = businessContextProvider;
        this.orderFactProvider = orderFactProvider;
        this.orderQuestionClassifier = orderQuestionClassifier;
        this.orderEntityReferenceResolver = orderEntityReferenceResolver;
        this.orderAiTools = orderAiTools;
    }

    @Override
    public AiSceneType sceneType() {
        return AiSceneType.ORDER;
    }

    @Override
    public AiPromptPlan buildPlan(AiSceneRequestContext context) {
        OrderQuestionType questionType = orderQuestionClassifier.classify(context.message());

        return switch (questionType) {
            case DETAIL_QUERY -> buildDetailPlan(context);
            case STATUS_RULE -> new AiPromptPlan(
                    buildStatusPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_order_status",
                    buildStatusCard()
            );
            case REALTIME_STATS -> new AiPromptPlan(
                    buildRealtimeToolPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_order_module",
                    buildRealtimeToolCard(),
                    new Object[]{orderAiTools},
                    buildToolContext(context)
            );
            case GENERAL_ORDER -> new AiPromptPlan(
                    buildGeneralPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_order_module",
                    new AiDisplayCard(
                            "订单助手",
                            "order",
                            "当前回答围绕订单领域的一般问题生成。",
                            List.of()
                    )
            );
        };
    }

    private AiPromptPlan buildDetailPlan(AiSceneRequestContext context) {
        AiEntityReference reference = orderEntityReferenceResolver.resolve(context.message());
        if (reference == null || reference.getEntityId() == null) {
            return new AiPromptPlan(
                    buildGeneralPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "ask_more_details",
                    new AiDisplayCard(
                            "订单查询",
                            "order-detail",
                            "未能从问题中提取订单编号。",
                            List.of()
                    )
            );
        }

        String prompt = """
                %s

                下面是订单领域的静态业务事实：
                %s

                当前用户问题的查询重点是：
                %s

                你现在是 EFoodPass 的订单助手。
                用户已经给出了明确的订单ID。只要问题涉及订单状态、订单金额、菜品明细或订单详情，
                你就应该调用工具 `getAccessibleOrderDetail` 获取真实数据，不要根据静态事实猜测动态结果。

                如果工具返回：
                1. status = success：基于真实订单数据回答
                2. status = not_found：明确说明订单不存在
                3. status = restricted：明确说明当前登录用户无权查看该订单

                不要编造任何订单详情。
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts(),
                buildIntentHint(reference)
        );

        return new AiPromptPlan(
                prompt,
                AiAnswerType.NORMAL,
                true,
                resolveDetailAction(reference),
                buildDetailToolCard(reference),
                new Object[]{orderAiTools},
                buildToolContext(context)
        );
    }

    private String buildGeneralPrompt() {
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
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts()
        );
    }

    private String buildStatusPrompt() {
        return """
                %s

                下面是订单领域的静态业务事实：
                %s

                你现在是 EFoodPass 的订单助手。
                当前问题更偏向订单规则、订单状态定义或订单流程说明。
                这类问题优先基于静态业务事实回答，不需要调用工具。

                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts()
        );
    }

    private String buildRealtimeToolPrompt() {
        return """
                %s

                下面是订单领域的静态业务事实：
                %s

                你现在是 EFoodPass 的订单助手。
                当前问题更偏向订单统计、订单数量、金额汇总或整体情况。
                对于这类动态问题，你应该调用工具 `getOrderStatistics` 获取实时数据，
                不要根据静态事实猜测统计结果。

                如果工具返回 success，就基于工具结果回答。
                不要编造任何统计数字。

                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts()
        );
    }

    private String buildIntentHint(AiEntityReference reference) {
        return switch (reference.getIntent()) {
            case STATUS -> "用户重点想知道该订单当前状态";
            case AMOUNT -> "用户重点想知道该订单金额信息";
            case ITEMS -> "用户重点想知道该订单的菜品明细";
            case DETAIL -> "用户重点想知道该订单的完整详情";
            default -> "用户重点想了解该订单的基本信息";
        };
    }

    private String resolveDetailAction(AiEntityReference reference) {
        return switch (reference.getIntent()) {
            case STATUS -> "view_order_status";
            case ITEMS, DETAIL, AMOUNT -> "view_order_detail";
            default -> "view_order_module";
        };
    }

    private Map<String, Object> buildToolContext(AiSceneRequestContext context) {
        return Map.of(
                AiToolContextKeys.CURRENT_USER_ID, context.currentUserId(),
                AiToolContextKeys.CAN_VIEW_ANY_ORDER, context.canViewAnyOrder()
        );
    }

    private AiDisplayCard buildStatusCard() {
        return new AiDisplayCard(
                "订单状态说明",
                "order-status",
                "当前回答围绕订单状态定义与规则生成。",
                Arrays.stream(FoodOrderStatus.values())
                        .map(status -> new AiDisplayField("状态 " + status.getCode(), status.getLabel()))
                        .toList()
        );
    }

    private AiDisplayCard buildRealtimeToolCard() {
        return new AiDisplayCard(
                "订单实时统计",
                "order-stats",
                "这类问题会由模型按需调用订单统计工具获取实时数据。",
                List.of(
                        new AiDisplayField("数据来源", "getOrderStatistics 工具"),
                        new AiDisplayField("查询方式", "模型按需调用")
                )
        );
    }

    private AiDisplayCard buildDetailToolCard(AiEntityReference reference) {
        return new AiDisplayCard(
                "订单详情查询",
                "order-detail",
                "这类问题会由模型按需调用订单详情工具获取真实数据。",
                List.of(
                        new AiDisplayField("订单ID", String.valueOf(reference.getEntityId())),
                        new AiDisplayField("查询重点", buildIntentHint(reference)),
                        new AiDisplayField("数据来源", "getAccessibleOrderDetail 工具")
                )
        );
    }
}
