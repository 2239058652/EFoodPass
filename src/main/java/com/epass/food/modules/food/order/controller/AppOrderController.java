package com.epass.food.modules.food.order.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.food.order.dto.AppOrderCreateRequest;
import com.epass.food.modules.food.order.dto.FoodOrderDetailResponse;
import com.epass.food.modules.food.order.dto.FoodOrderListQuery;
import com.epass.food.modules.food.order.dto.FoodOrderListResponse;
import com.epass.food.modules.food.order.service.FoodOrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/app/order")
public class AppOrderController {

    private final FoodOrderService foodOrderService;

    public AppOrderController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    @GetMapping("/list")
    public Result<PageResult<FoodOrderListResponse>> list(FoodOrderListQuery query,
                                                          Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(foodOrderService.listCurrentUserOrders(loginUser.getUserId(), query));
    }

    @GetMapping("/{id}")
    public Result<FoodOrderDetailResponse> detail(@PathVariable Long id,
                                                  Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(foodOrderService.getCurrentUserOrderDetail(loginUser.getUserId(), id));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody AppOrderCreateRequest request,
                               Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        foodOrderService.createCurrentUserOrder(loginUser.getUserId(), request);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id,
                               Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        foodOrderService.cancelCurrentUserOrder(loginUser.getUserId(), id);
        return Result.success();
    }
}