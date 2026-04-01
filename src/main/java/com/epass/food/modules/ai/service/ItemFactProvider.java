package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class ItemFactProvider {

    public String buildItemFacts() {
        return """
                当前菜品领域的真实业务事实：
                1. 菜品管理模块是 food/item
                2. 支持菜品列表、详情、新增、修改、删除
                3. 支持菜品上架和下架
                4. 支持手动调整库存
                5. 菜品依赖菜品分类
                """;
    }
}