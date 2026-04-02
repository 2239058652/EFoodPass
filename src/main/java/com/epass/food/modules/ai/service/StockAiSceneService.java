package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiDisplayField;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import org.springframework.stereotype.Service;

@Service
public class StockAiSceneService implements AiSceneHandler {

    private final BusinessContextProvider businessContextProvider;
    private final StockFactProvider stockFactProvider;
    private final StockChangeSceneCatalog stockChangeSceneCatalog;

    public StockAiSceneService(BusinessContextProvider businessContextProvider,
                               StockFactProvider stockFactProvider,
                               StockChangeSceneCatalog stockChangeSceneCatalog) {
        this.businessContextProvider = businessContextProvider;
        this.stockFactProvider = stockFactProvider;
        this.stockChangeSceneCatalog = stockChangeSceneCatalog;
    }

    @Override
    public AiSceneType sceneType() {
        return AiSceneType.STOCK;
    }

    @Override
    public AiPromptPlan buildPlan(AiSceneRequestContext context) {
        return new AiPromptPlan(
                buildPrompt(),
                AiAnswerType.NORMAL,
                true,
                "view_stock_module",
                buildCard()
        );
    }

    private String buildPrompt() {
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

    private AiDisplayCard buildCard() {
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
