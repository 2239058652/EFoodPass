package com.epass.food.modules.food.order.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.food.order.dto.OrderDailyAmountResponse;
import com.epass.food.modules.food.order.dto.OrderStatOverviewResponse;
import com.epass.food.modules.food.order.dto.OrderStatusCountResponse;
import com.epass.food.modules.food.order.dto.OrderTopItemResponse;
import com.epass.food.modules.food.order.service.FoodOrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/food/order/stat")
public class FoodOrderStatController {

    private final FoodOrderService foodOrderService;

    public FoodOrderStatController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/overview")
    public Result<OrderStatOverviewResponse> overview() {
        return Result.success(foodOrderService.getOrderStatOverview());
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/status-count")
    public Result<List<OrderStatusCountResponse>> statusCount() {
        return Result.success(foodOrderService.getOrderStatusCounts());
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/top-item")
    public Result<List<OrderTopItemResponse>> topItem() {
        return Result.success(foodOrderService.getTopSellingItems());
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/daily-amount")
    public Result<List<OrderDailyAmountResponse>> dailyAmount() {
        return Result.success(foodOrderService.getDailyAmounts());
    }
}