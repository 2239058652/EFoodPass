package com.epass.food.modules.ai.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.ItemToolDetailResult;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.enums.FoodItemSaleStatus;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ItemAiTools {

    private final ItemAiSupportService itemAiSupportService;

    public ItemAiTools(ItemAiSupportService itemAiSupportService) {
        this.itemAiSupportService = itemAiSupportService;
    }

    @Tool(
            name = "getItemDetail",
            description = "根据菜品ID查询菜品详情。仅当用户明确提供菜品ID，并询问菜品状态、库存、分类或菜品详情时调用。"
    )
    public ItemToolDetailResult getItemDetail(
            @ToolParam(description = "要查询的菜品ID") Long itemId
    ) {
        try {
            FoodItem item = itemAiSupportService.getRequiredItem(itemId);
            return new ItemToolDetailResult(
                    "success",
                    "已成功获取菜品详情。",
                    item.getId(),
                    item.getCategoryId(),
                    item.getName(),
                    String.valueOf(item.getPrice()),
                    item.getStock(),
                    item.getIsOnSale(),
                    FoodItemSaleStatus.getLabelByCode(item.getIsOnSale()),
                    item.getDescription()
            );
        } catch (BusinessException e) {
            return new ItemToolDetailResult(
                    "not_found",
                    "该菜品不存在。",
                    itemId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }
}
