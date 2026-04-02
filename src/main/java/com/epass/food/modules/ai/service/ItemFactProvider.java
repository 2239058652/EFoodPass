package com.epass.food.modules.ai.service;

import com.epass.food.modules.food.item.enums.FoodItemSaleStatus;
import org.springframework.stereotype.Component;

@Component
public class ItemFactProvider {

    public String buildItemFacts() {
        String statusFacts = java.util.Arrays.stream(FoodItemSaleStatus.values())
                .map(status -> "- %d：%s".formatted(status.getCode(), status.getLabel()))
                .collect(java.util.stream.Collectors.joining("\n"));

        return """
                当前菜品领域的真实业务事实：
                1. 菜品管理模块是 food/item
                2. 支持菜品列表、详情、新增、修改、删除
                3. 菜品上下架状态如下：
                %s
                4. 支持手动调整库存
                5. 菜品依赖菜品分类
                """.formatted(statusFacts);
    }
}