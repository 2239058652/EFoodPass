package com.epass.food.modules.food.order.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.food.order.dto.*;
import com.epass.food.modules.food.order.service.FoodOrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/food/order")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    public FoodOrderController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    @PreAuthorize("hasAuthority('food:order:list')")
    @GetMapping("/list")
    public Result<PageResult<FoodOrderListResponse>> list(FoodOrderListQuery query) {
        return Result.success(foodOrderService.listOrders(query));
    }

    @PreAuthorize("hasAuthority('food:order:detail')")
    @GetMapping("/{id}")
    public Result<FoodOrderDetailResponse> detail(@PathVariable Long id) {
        return Result.success(foodOrderService.getOrderDetail(id));
    }

    @PreAuthorize("hasAuthority('food:order:add')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody FoodOrderCreateRequest request) {
        foodOrderService.createOrder(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:order:process')")
    @PutMapping("/process")
    public Result<Void> process(@Valid @RequestBody FoodOrderUpdateStatusRequest request) {
        foodOrderService.processOrder(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:order:cancel')")
    @PutMapping("/cancel")
    public Result<Void> cancel(@Valid @RequestBody FoodOrderUpdateStatusRequest request) {
        foodOrderService.cancelOrder(request);
        return Result.success();
    }

    @PreAuthorize("hasAuthority('food:order:complete')")
    @PutMapping("/complete")
    public Result<Void> complete(@Valid @RequestBody FoodOrderUpdateStatusRequest request) {
        foodOrderService.completeOrder(request);
        return Result.success();
    }
}