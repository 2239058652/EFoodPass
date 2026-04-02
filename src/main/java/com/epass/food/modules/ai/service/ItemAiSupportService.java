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
}
