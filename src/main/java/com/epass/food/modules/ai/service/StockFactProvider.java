package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class StockFactProvider {

    private final StockChangeSceneCatalog stockChangeSceneCatalog;

    public StockFactProvider(StockChangeSceneCatalog stockChangeSceneCatalog) {
        this.stockChangeSceneCatalog = stockChangeSceneCatalog;
    }

    public String buildStockFacts() {
        String scenes = stockChangeSceneCatalog.getScenes().stream()
                .map(scene -> "- %s：%s".formatted(scene.code(), scene.label()))
                .collect(java.util.stream.Collectors.joining("\n"));

        return """
                当前库存领域的真实业务事实：
                1. 库存日志模块是 food/stock
                2. 当前库存日志覆盖以下场景：
                %s
                3. 支持库存日志查询
                """.formatted(scenes);
    }

}