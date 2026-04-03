package com.epass.food.modules.food.order.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.food.order.dto.OrderDailyAmountResponse;
import com.epass.food.modules.food.order.dto.OrderPaymentStatusCountResponse;
import com.epass.food.modules.food.order.dto.OrderStatQuery;
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
    public Result<OrderStatOverviewResponse> overview(OrderStatQuery query) {
        return Result.success(foodOrderService.getOrderStatOverview(query));
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/status-count")
    public Result<List<OrderStatusCountResponse>> statusCount(OrderStatQuery query) {
        return Result.success(foodOrderService.getOrderStatusCounts(query));
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/payment-status-count")
    public Result<List<OrderPaymentStatusCountResponse>> paymentStatusCount(OrderStatQuery query) {
        return Result.success(foodOrderService.getOrderPaymentStatusCounts(query));
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/top-item")
    public Result<List<OrderTopItemResponse>> topItem(OrderStatQuery query) {
        return Result.success(foodOrderService.getTopSellingItems(query));
    }

    @PreAuthorize("hasAuthority('food:order:stat')")
    @GetMapping("/daily-amount")
    public Result<List<OrderDailyAmountResponse>> dailyAmount(OrderStatQuery query) {
        return Result.success(foodOrderService.getDailyAmounts(query));
    }
}
