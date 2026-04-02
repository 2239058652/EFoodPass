package com.epass.food.modules.ai.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import org.springframework.stereotype.Service;

@Service
public class ItemAiSupportService {

    private final FoodItemMapper foodItemMapper;

    public ItemAiSupportService(FoodItemMapper foodItemMapper) {
        this.foodItemMapper = foodItemMapper;
    }

    public FoodItem getRequiredItem(Long itemId) {
        FoodItem item = foodItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(BizErrorCode.ITEM_NOT_FOUND, "菜品不存在");
        }
        return item;
    }

    public String buildItemDetailFacts(Long itemId) {
        FoodItem item = getRequiredItem(itemId);

        return """
                当前查询的菜品真实详情如下：
                1. 菜品ID：%s
                2. 分类ID：%s
                3. 菜品名称：%s
                4. 当前价格：%s
                5. 当前库存：%s
                6. 上架状态：%s
                7. 菜品描述：%s
                """.formatted(
                item.getId(),
                item.getCategoryId(),
                item.getName(),
                item.getPrice(),
                item.getStock(),
                item.getIsOnSale(),
                item.getDescription() == null ? "无" : item.getDescription()
        );
    }

    public FoodItem getItemDetail(Long itemId) {
        return getRequiredItem(itemId);
    }
}