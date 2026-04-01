package com.epass.food.modules.ai.service;

import com.epass.food.modules.food.order.dto.OrderStatOverviewResponse;
import com.epass.food.modules.food.order.dto.OrderStatusCountResponse;
import com.epass.food.modules.food.order.service.FoodOrderService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderAiSupportService {

    private final FoodOrderService foodOrderService;

    public OrderAiSupportService(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    public String buildRealtimeOrderFacts() {
        OrderStatOverviewResponse overview = foodOrderService.getOrderStatOverview();
        List<OrderStatusCountResponse> statusCounts = foodOrderService.getOrderStatusCounts();

        String statusSummary = statusCounts.stream()
                .map(item -> "状态 %s 的订单数是 %s".formatted(item.getOrderStatus(), item.getOrderCount()))
                .collect(Collectors.joining("；"));

        return """
                当前系统中的实时订单数据如下：
                1. 订单总数：%s
                2. 待确认订单数：%s
                3. 制作中订单数：%s
                4. 已完成订单数：%s
                5. 已取消订单数：%s
                6. 订单总金额：%s
                7. 已完成订单金额：%s
                8. 状态统计明细：%s
                """.formatted(
                overview.getTotalOrderCount(),
                overview.getPendingOrderCount(),
                overview.getProcessingOrderCount(),
                overview.getCompletedOrderCount(),
                overview.getCanceledOrderCount(),
                overview.getTotalAmount(),
                overview.getCompletedAmount(),
                statusSummary
        );
    }

    public String buildOrderDetailFacts(Long orderId) {
        var detail = foodOrderService.getOrderDetail(orderId);

        String itemSummary;
        if (detail.getItems() == null || detail.getItems().isEmpty()) {
            itemSummary = "该订单没有菜品明细";
        } else {
            itemSummary = detail.getItems().stream()
                    .map(item -> "%s x%s，小计 %s"
                            .formatted(
                                    item.getFoodNameSnapshot(),
                                    item.getQuantity(),
                                    item.getAmount()
                            ))
                    .collect(java.util.stream.Collectors.joining("；"));
        }

        return """
                当前查询的订单真实详情如下：
                1. 订单ID：%s
                2. 订单编号：%s
                3. 用户ID：%s
                4. 订单状态：%s
                5. 订单总金额：%s
                6. 备注：%s
                7. 下单时间：%s
                8. 菜品明细：%s
                """.formatted(
                detail.getId(),
                detail.getOrderNo(),
                detail.getUserId(),
                detail.getOrderStatus(),
                detail.getTotalAmount(),
                detail.getRemark() == null ? "无" : detail.getRemark(),
                detail.getCreatedAt(),
                itemSummary
        );
    }
}