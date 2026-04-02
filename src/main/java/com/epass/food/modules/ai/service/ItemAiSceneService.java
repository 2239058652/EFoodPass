package com.epass.food.modules.ai.service;

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
import java.util.Map;

@Service
public class ItemAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;
    private final ItemFactProvider itemFactProvider;
    private final ItemQuestionClassifier itemQuestionClassifier;
    private final ItemEntityReferenceResolver itemEntityReferenceResolver;
    private final ItemAiTools itemAiTools;

    public ItemAiSceneService(BusinessContextProvider businessContextProvider,
                              ItemFactProvider itemFactProvider,
                              ItemQuestionClassifier itemQuestionClassifier,
                              ItemEntityReferenceResolver itemEntityReferenceResolver,
                              ItemAiTools itemAiTools) {
        this.businessContextProvider = businessContextProvider;
        this.itemFactProvider = itemFactProvider;
        this.itemQuestionClassifier = itemQuestionClassifier;
        this.itemEntityReferenceResolver = itemEntityReferenceResolver;
        this.itemAiTools = itemAiTools;
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
                    buildStatusCard(),
                    new Object[0],
                    Map.of(),
                    structuredParams("content").toMap()
            );
            case GENERAL_ITEM -> new AiPromptPlan(
                    buildGeneralPrompt(),
                    AiAnswerType.NORMAL,
                    true,
                    "view_item_module",
                    new AiDisplayCard("菜品助手", "item", "当前回答围绕菜品领域的一般问题生成。", List.of()),
                    new Object[0],
                    Map.of(),
                    structuredParams("content").toMap()
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
                    new AiDisplayCard("菜品查询", "item-detail", "未能从问题中提取菜品编号。", List.of()),
                    new Object[0],
                    Map.of(),
                    structuredParams("content").toMap()
            );
        }

        String prompt = """
                %s

                下面是菜品领域的真实业务事实：
                %s

                当前用户问题的查询重点是：
                %s

                你现在是 EFoodPass 的菜品助手。
                用户已经给出了明确的菜品ID。只要问题涉及菜品状态、库存、分类或菜品详情，
                你就应该调用工具 `getItemDetail` 获取真实数据，不要根据静态事实猜测动态结果。

                如果工具返回：
                1. status = success：基于真实菜品数据回答
                2. status = not_found：明确说明菜品不存在

                不要编造任何菜品详情。
                """.formatted(
                businessContextProvider.buildCommonFacts(),
                itemFactProvider.buildItemFacts(),
                buildIntentHint(reference)
        );

        return new AiPromptPlan(
                prompt,
                AiAnswerType.NORMAL,
                true,
                resolveDetailAction(reference),
                buildDetailToolCard(reference),
                new Object[]{itemAiTools},
                Map.of(),
                structuredParams("content", "toolStatus")
                        .withToolStatus("success", "not_found")
                        .toMap()
        );
    }

    private String buildStatusPrompt() {
        return """
                %s

                下面是菜品领域的真实业务事实：
                %s

                当前问题更偏向菜品状态规则说明。
                这类问题优先基于静态业务事实回答，不需要调用工具。
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

    private AiDisplayCard buildDetailToolCard(AiEntityReference reference) {
        return new AiDisplayCard(
                "菜品详情查询",
                "item-detail",
                "这类问题会由模型按需调用菜品详情工具获取真实数据。",
                List.of(
                        new AiDisplayField("菜品ID", String.valueOf(reference.getEntityId())),
                        new AiDisplayField("查询重点", buildIntentHint(reference)),
                        new AiDisplayField("数据来源", "getItemDetail 工具")
                )
        );
    }

    private StructuredOutputParams structuredParams(String... fields) {
        return new StructuredOutputParams(fields);
    }

    private static final class StructuredOutputParams {

        private final List<String> fields;
        private List<String> toolStatusOptions = List.of();

        private StructuredOutputParams(String... fields) {
            this.fields = List.of(fields);
        }

        private StructuredOutputParams withToolStatus(String... options) {
            this.toolStatusOptions = List.of(options);
            return this;
        }

        private Map<String, Object> toMap() {
            if (toolStatusOptions.isEmpty()) {
                return Map.of(AiAdvisorContextKeys.STRUCTURED_FIELDS, fields);
            }
            return Map.of(
                    AiAdvisorContextKeys.STRUCTURED_FIELDS, fields,
                    AiAdvisorContextKeys.TOOL_STATUS_OPTIONS, toolStatusOptions
            );
        }
    }
}
