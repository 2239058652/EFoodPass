package com.epass.food.modules.food.item.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.item.dto.*;
import com.epass.food.modules.food.item.entity.FoodItem;

public interface FoodItemService extends IService<FoodItem> {

    PageResult<FoodItemListResponse> listItems(FoodItemListQuery query);

    FoodItemDetailResponse getItemDetail(Long itemId);

    void createItem(FoodItemCreateRequest request);

    void updateItem(FoodItemUpdateRequest request);

    void updateOnSaleStatus(FoodItemUpdateOnSaleRequest request);

    void deleteItem(Long itemId);
}