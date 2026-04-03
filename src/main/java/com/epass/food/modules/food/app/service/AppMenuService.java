package com.epass.food.modules.food.app.service;

import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.app.dto.AppMenuCategoryResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemDetailResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemListQuery;
import com.epass.food.modules.food.app.dto.AppMenuItemResponse;

import java.util.List;

public interface AppMenuService {

    List<AppMenuCategoryResponse> listMenuTree();

    PageResult<AppMenuItemResponse> listAvailableItems(AppMenuItemListQuery query);

    AppMenuItemDetailResponse getAvailableItemDetail(Long itemId);
}
