package com.epass.food.modules.food.app.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.food.app.dto.AppMenuCategoryResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemDetailResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemListQuery;
import com.epass.food.modules.food.app.dto.AppMenuItemResponse;
import com.epass.food.modules.food.app.service.AppMenuService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app/menu")
public class AppMenuController {

    private final AppMenuService appMenuService;

    public AppMenuController(AppMenuService appMenuService) {
        this.appMenuService = appMenuService;
    }

    @GetMapping("/tree")
    public Result<List<AppMenuCategoryResponse>> tree() {
        return Result.success(appMenuService.listMenuTree());
    }

    @GetMapping("/items")
    public Result<PageResult<AppMenuItemResponse>> items(AppMenuItemListQuery query) {
        return Result.success(appMenuService.listAvailableItems(query));
    }

    @GetMapping("/item/{id}")
    public Result<AppMenuItemDetailResponse> itemDetail(@PathVariable Long id) {
        return Result.success(appMenuService.getAvailableItemDetail(id));
    }
}
