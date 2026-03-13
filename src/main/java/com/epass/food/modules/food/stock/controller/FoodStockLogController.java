package com.epass.food.modules.food.stock.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.food.stock.dto.FoodStockLogListQuery;
import com.epass.food.modules.food.stock.dto.FoodStockLogListResponse;
import com.epass.food.modules.food.stock.service.FoodStockLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/food/stock-log")
public class FoodStockLogController {

    private final FoodStockLogService foodStockLogService;

    public FoodStockLogController(FoodStockLogService foodStockLogService) {
        this.foodStockLogService = foodStockLogService;
    }

    @PreAuthorize("hasAuthority('food:stock-log:list')")
    @GetMapping("/list")
    public Result<PageResult<FoodStockLogListResponse>> list(FoodStockLogListQuery query) {
        return Result.success(foodStockLogService.listLogs(query));
    }
}