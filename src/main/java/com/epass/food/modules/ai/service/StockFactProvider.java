package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class StockFactProvider {

    public String buildStockFacts() {
        return """
                当前库存领域的真实业务事实：
                1. 库存日志模块是 food/stock
                2. 当前库存日志主要覆盖：
                   - 下单扣减
                   - 取消回补
                   - 后台手动调整
                3. 支持库存日志查询
                """;
    }
}