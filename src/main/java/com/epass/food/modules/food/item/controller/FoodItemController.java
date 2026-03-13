package com.epass.food.modules.food.item.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.food.item.dto.*;
import com.epass.food.modules.food.item.service.FoodItemService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/food/item")
public class FoodItemController {

    private final FoodItemService foodItemService;

    public FoodItemController(FoodItemService foodItemService) {
        this.foodItemService = foodItemService;
    }

    @PreAuthorize("hasAuthority('food:item:list')")
    @GetMapping("/list")
    public Result<PageResult<FoodItemListResponse>> list(FoodItemListQuery query) {
        return Result.success(foodItemService.listItems(query));
    }

    @PreAuthorize("hasAuthority('food:item:detail')")
    @GetMapping("/{id}")
    public Result<FoodItemDetailResponse> detail(@PathVariable Long id) {
        return Result.success(foodItemService.getItemDetail(id));
    }

    @PreAuthorize("hasAuthority('food:item:add')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody FoodItemCreateRequest request) {
        foodItemService.createItem(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:item:update')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody FoodItemUpdateRequest request) {
        foodItemService.updateItem(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:item:update-on-sale')")
    @PutMapping("/on-sale")
    public Result<Void> updateOnSale(@Valid @RequestBody FoodItemUpdateOnSaleRequest request) {
        foodItemService.updateOnSaleStatus(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:item:update-stock')")
    @PutMapping("/stock")
    public Result<Void> adjustStock(@Valid @RequestBody FoodItemAdjustStockRequest request) {
        foodItemService.adjustStock(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:item:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        foodItemService.deleteItem(id);
        return Result.success();
    }
}