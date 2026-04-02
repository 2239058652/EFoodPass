package com.epass.food.modules.ai.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiDisplayField;
import com.epass.food.modules.ai.dto.AiEntityReference;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.dto.ItemQuestionType;
import com.epass.food.modules.food.item.enums.FoodItemSaleStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class ItemAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;
    private final ItemFactProvider itemFactProvider;
    private final ItemQuestionClassifier itemQuestionClassifier;
    private final ItemEntityReferenceResolver itemEntityReferenceResolver;
    private final ItemAiSupportService itemAiSupportService;

    public ItemAiSceneService(BusinessContextProvider businessContextProvider,
                              ItemFactProvider itemFactProvider,
                              ItemQuestionClassifier itemQuestionClassifier,
                              ItemEntityReferenceResolver itemEntityReferenceResolver,
                              ItemAiSupportService itemAiSupportService) {
        this.businessContextProvider = businessContextProvider;
        this.itemFactProvider = itemFactProvider;
        this.itemQuestionClassifier = itemQuestionClassifier;
        this.itemEntityReferenceResolver = itemEntityReferenceResolver;
        this.itemAiSupportService = itemAiSupportService;
    }

    @Override
    public AiSceneType sceneType() {
        return AiSceneType.ITEM;
    }

    @Override
    public AiPromptPlan buildPlan(AiSceneRequestContext context) {
        ItemQuestionType questionType = itemQuestionClassifier.classify(context.message());

        return switch (questionType) {
            case DETAIL_QUERY -> buildDetailPlan(context.message());
            case STATUS_RULE -> new AiPromptPlan(
                    buildStatusPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_item_status",
                    buildStatusCard()
            );
            case GENERAL_ITEM -> new AiPromptPlan(
                    buildGeneralPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_item_module",
                    new AiDisplayCard("菜品助手", "item", "当前回答围绕菜品领域的一般问题生成。", List.of())
            );
        };
    }

    private AiPromptPlan buildDetailPlan(String message) {
        AiEntityReference reference = itemEntityReferenceResolver.resolve(message);
        if (reference == null || reference.getEntityId() == null) {
            return new AiPromptPlan(
                    buildGeneralPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "ask_more_details",
                    new AiDisplayCard("菜品助手", "item", "未能从问题中提取菜品编号。", List.of())
            );
        }

        try {
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
                    itemAiSupportService.buildItemDetailFacts(reference.getEntityId()),
                    buildIntentHint(reference)
            );

            return new AiPromptPlan(
                    prompt,
                    AiAnswerType.NORMAL,
                    true,
                    resolveDetailAction(reference),
                    buildDetailCard(reference)
            );
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

            return new AiPromptPlan(
                    prompt,
                    AiAnswerType.NOT_FOUND,
                    true,
                    "ask_more_details",
                    new AiDisplayCard("未找到菜品", "item-not-found", "当前查询的菜品不存在，请确认菜品编号。", List.of())
            );
        }
    }

    private String buildStatusPrompt() {
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

    private String buildGeneralPrompt() {
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

    private String buildIntentHint(AiEntityReference reference) {
        return switch (reference.getIntent()) {
            case STATUS -> "用户重点想知道该菜品当前上架状态";
            case STOCK -> "用户重点想知道该菜品当前库存";
            case CATEGORY -> "用户重点想知道该菜品所属分类";
            case DETAIL -> "用户重点想知道该菜品完整详情";
            default -> "用户重点想了解该菜品的基本信息";
        };
    }

    private String resolveDetailAction(AiEntityReference reference) {
        return switch (reference.getIntent()) {
            case STATUS -> "view_item_status";
            case STOCK, CATEGORY, DETAIL -> "view_item_detail";
            default -> "view_item_module";
        };
    }

    private AiDisplayCard buildStatusCard() {
        return new AiDisplayCard(
                "菜品状态说明",
                "item-status",
                "当前卡片展示菜品上下架状态。",
                Arrays.stream(FoodItemSaleStatus.values())
                        .map(status -> new AiDisplayField("状态 " + status.getCode(), status.getLabel()))
                        .toList()
        );
    }

    private AiDisplayCard buildDetailCard(AiEntityReference reference) {
        if (reference.getEntityId() == null) {
            return new AiDisplayCard("菜品详情", "item-detail", "未能从问题中提取菜品编号。", List.of());
        }

        try {
            var item = itemAiSupportService.getItemDetail(reference.getEntityId());

            return new AiDisplayCard(
                    "菜品详情",
                    "item-detail",
                    "当前卡片展示指定菜品的关键字段。",
                    List.of(
                            new AiDisplayField("菜品ID", String.valueOf(item.getId())),
                            new AiDisplayField("菜品名称", item.getName()),
                            new AiDisplayField("分类ID", String.valueOf(item.getCategoryId())),
                            new AiDisplayField("当前价格", String.valueOf(item.getPrice())),
                            new AiDisplayField("当前库存", String.valueOf(item.getStock())),
                            new AiDisplayField(
                                    "上架状态",
                                    item.getIsOnSale() + "（" + FoodItemSaleStatus.getLabelByCode(item.getIsOnSale()) + "）"
                            )
                    )
            );
        } catch (BusinessException e) {
            return new AiDisplayCard("菜品详情", "item-detail", "当前无法展示菜品关键字段。", List.of());
        }
    }
}
