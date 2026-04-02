package com.epass.food.modules.ai.service;

import com.epass.food.modules.food.order.dto.FoodOrderDetailResponse;
import com.epass.food.modules.food.order.dto.OrderStatOverviewResponse;
import com.epass.food.modules.food.order.service.FoodOrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderAiSupportService {

    private final FoodOrderService foodOrderService;

    public OrderAiSupportService(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    public FoodOrderDetailResponse getAccessibleOrderDetail(Long currentUserId,
                                                            boolean canViewAnyOrder,
                                                            Long orderId) {
        return canViewAnyOrder
                ? foodOrderService.getOrderDetail(orderId)
                : foodOrderService.getCurrentUserOrderDetail(currentUserId, orderId);
    }

    public OrderStatOverviewResponse getOrderStatOverview() {
        return foodOrderService.getOrderStatOverview();
    }
}
