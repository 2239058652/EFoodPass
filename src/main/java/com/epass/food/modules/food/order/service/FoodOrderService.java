package com.epass.food.modules.food.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.order.dto.*;
import com.epass.food.modules.food.order.entity.FoodOrder;

public interface FoodOrderService extends IService<FoodOrder> {

    PageResult<FoodOrderListResponse> listOrders(FoodOrderListQuery query);

    FoodOrderDetailResponse getOrderDetail(Long orderId);

    void createOrder(FoodOrderCreateRequest request);

    void processOrder(FoodOrderUpdateStatusRequest request);

    void cancelOrder(FoodOrderUpdateStatusRequest request);

    void completeOrder(FoodOrderUpdateStatusRequest request);

    PageResult<FoodOrderListResponse> listCurrentUserOrders(Long userId, FoodOrderListQuery query);

    FoodOrderDetailResponse getCurrentUserOrderDetail(Long userId, Long orderId);

    void createCurrentUserOrder(Long userId, AppOrderCreateRequest request);

    void cancelCurrentUserOrder(Long userId, Long orderId);
}