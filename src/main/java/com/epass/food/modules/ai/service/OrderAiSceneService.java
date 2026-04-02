package com.epass.food.modules.ai.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.BizErrorCode;
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

@Service
public class OrderAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;
    private final OrderFactProvider orderFactProvider;
    private final OrderAiSupportService orderAiSupportService;
    private final OrderQuestionClassifier orderQuestionClassifier;
    private final OrderEntityReferenceResolver orderEntityReferenceResolver;

    public OrderAiSceneService(BusinessContextProvider businessContextProvider,
                               OrderFactProvider orderFactProvider,
                               OrderAiSupportService orderAiSupportService,
                               OrderQuestionClassifier orderQuestionClassifier,
                               OrderEntityReferenceResolver orderEntityReferenceResolver) {
        this.businessContextProvider = businessContextProvider;
        this.orderFactProvider = orderFactProvider;
        this.orderAiSupportService = orderAiSupportService;
        this.orderQuestionClassifier = orderQuestionClassifier;
        this.orderEntityReferenceResolver = orderEntityReferenceResolver;
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
                    buildRealtimePrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_order_module",
                    buildStatsCard()
            );
            case GENERAL_ORDER -> new AiPromptPlan(
                    buildGeneralPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_order_module",
                    new AiDisplayCard("订单助手", "order", "当前回答围绕订单领域的一般问题生成。", List.of())
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
                    new AiDisplayCard("订单助手", "order", "未能从问题中提取订单编号。", List.of())
            );
        }

        try {
            String detailFacts = orderAiSupportService.buildOrderDetailFacts(
                    context.currentUserId(),
                    context.canViewAnyOrder(),
                    reference.getEntityId()
            );

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
                    buildIntentHint(reference)
            );

            return new AiPromptPlan(
                prompt,
                AiAnswerType.NORMAL,
                true,
                resolveDetailAction(reference),
                buildDetailCard(reference, context.currentUserId(), context.canViewAnyOrder())
            );
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
                    buildAccessHint(answerType)
            );

            return new AiPromptPlan(
                    prompt,
                    answerType,
                    true,
                    "ask_more_details",
                    buildFailureCard(answerType)
            );
        }
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

    private String buildRealtimePrompt() {
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
                  "content": "给用户的中文回答"
                }
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                orderFactProvider.buildOrderFacts(),
                orderAiSupportService.buildRealtimeOrderFacts()
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

    private String buildAccessHint(AiAnswerType answerType) {
        return switch (answerType) {
            case NOT_FOUND -> "该订单不存在";
            case RESTRICTED -> "当前登录用户无权查看该订单";
            case NORMAL -> "订单详情可正常访问";
        };
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

    private AiDisplayCard buildStatsCard() {
        var overview = orderAiSupportService.getOrderStatOverview();

        return new AiDisplayCard(
                "订单实时统计",
                "order-stats",
                "当前卡片展示订单统计关键指标。",
                List.of(
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

    private AiDisplayCard buildDetailCard(AiEntityReference reference, Long currentUserId, boolean canViewAnyOrder) {
        if (reference.getEntityId() == null) {
            return new AiDisplayCard("订单详情", "order-detail", "未能从问题中提取订单编号。", List.of());
        }

        try {
            var detail = orderAiSupportService.getAccessibleOrderDetail(currentUserId, canViewAnyOrder, reference.getEntityId());

            return new AiDisplayCard(
                    "订单详情",
                    "order-detail",
                    "当前卡片展示指定订单的关键字段。",
                    List.of(
                            new AiDisplayField("订单ID", String.valueOf(detail.getId())),
                            new AiDisplayField("订单编号", detail.getOrderNo()),
                            new AiDisplayField("用户ID", String.valueOf(detail.getUserId())),
                            new AiDisplayField("订单状态", detail.getOrderStatus() + "（" + FoodOrderStatus.getLabelByCode(detail.getOrderStatus()) + "）"),
                            new AiDisplayField("总金额", String.valueOf(detail.getTotalAmount())),
                            new AiDisplayField("下单时间", String.valueOf(detail.getCreatedAt()))
                    )
            );
        } catch (BusinessException e) {
            return new AiDisplayCard("订单详情", "order-detail", "当前无法展示订单关键字段。", List.of());
        }
    }

    private AiDisplayCard buildFailureCard(AiAnswerType answerType) {
        return switch (answerType) {
            case NOT_FOUND -> new AiDisplayCard("未找到目标数据", "not-found", "当前查询目标不存在，请确认输入的信息是否正确。", List.of());
            case RESTRICTED -> new AiDisplayCard("访问受限", "restricted", "当前登录用户无权访问该数据，或系统不允许返回详情。", List.of());
            case NORMAL -> new AiDisplayCard("订单详情", "order-detail", "当前无法展示订单关键字段。", List.of());
        };
    }
}
