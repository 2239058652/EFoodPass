package com.epass.food.modules.ai.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.OrderToolDetailResult;
import com.epass.food.modules.ai.dto.OrderToolItemResult;
import com.epass.food.modules.ai.dto.OrderToolStatsResult;
import com.epass.food.modules.food.order.dto.FoodOrderDetailResponse;
import com.epass.food.modules.food.order.dto.OrderStatOverviewResponse;
import com.epass.food.modules.food.order.enums.FoodOrderStatus;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderAiTools {

    private final OrderAiSupportService orderAiSupportService;

    public OrderAiTools(OrderAiSupportService orderAiSupportService) {
        this.orderAiSupportService = orderAiSupportService;
    }

    @Tool(
            name = "getAccessibleOrderDetail",
            description = "根据订单ID查询当前登录用户有权访问的订单详情。仅当用户明确提供订单ID，并询问订单状态、金额、菜品明细或订单详情时调用。"
    )
    public OrderToolDetailResult getAccessibleOrderDetail(
            @ToolParam(description = "要查询的订单ID") Long orderId,
            ToolContext toolContext
    ) {
        Long currentUserId = readCurrentUserId(toolContext);
        boolean canViewAnyOrder = readCanViewAnyOrder(toolContext);

        try {
            FoodOrderDetailResponse detail = orderAiSupportService.getAccessibleOrderDetail(
                    currentUserId,
                    canViewAnyOrder,
                    orderId
            );

            List<OrderToolItemResult> items = detail.getItems() == null
                    ? List.of()
                    : detail.getItems().stream()
                    .map(item -> new OrderToolItemResult(
                            item.getFoodItemId(),
                            item.getFoodNameSnapshot(),
                            item.getQuantity(),
                            String.valueOf(item.getAmount())
                    ))
                    .toList();

            return new OrderToolDetailResult(
                    "success",
                    "已成功获取订单详情。",
                    detail.getId(),
                    detail.getOrderNo(),
                    detail.getUserId(),
                    detail.getOrderStatus(),
                    FoodOrderStatus.getLabelByCode(detail.getOrderStatus()),
                    String.valueOf(detail.getTotalAmount()),
                    detail.getRemark(),
                    String.valueOf(detail.getCreatedAt()),
                    items
            );
        } catch (BusinessException e) {
            return new OrderToolDetailResult(
                    resolveStatus(e),
                    resolveMessage(e),
                    orderId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of()
            );
        }
    }

    @Tool(
            name = "getOrderStatistics",
            description = "查询当前系统的订单统计概览，包括订单总数、各状态订单数以及金额汇总。仅当用户询问当前订单整体情况、订单数量、金额或实时统计时调用。"
    )
    public OrderToolStatsResult getOrderStatistics() {
        OrderStatOverviewResponse overview = orderAiSupportService.getOrderStatOverview();
        return new OrderToolStatsResult(
                "success",
                "已成功获取订单统计。",
                overview.getTotalOrderCount(),
                overview.getPendingOrderCount(),
                overview.getProcessingOrderCount(),
                overview.getCompletedOrderCount(),
                overview.getCanceledOrderCount(),
                String.valueOf(overview.getTotalAmount()),
                String.valueOf(overview.getCompletedAmount())
        );
    }

    private Long readCurrentUserId(ToolContext toolContext) {
        Object value = toolContext.getContext().get(AiToolContextKeys.CURRENT_USER_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("ToolContext 缺少 currentUserId");
    }

    private boolean readCanViewAnyOrder(ToolContext toolContext) {
        Object value = toolContext.getContext().get(AiToolContextKeys.CAN_VIEW_ANY_ORDER);
        return value instanceof Boolean bool && bool;
    }

    private String resolveStatus(BusinessException e) {
        String message = e.getMessage();
        if (message == null) {
            return "restricted";
        }
        if (message.contains("不存在")) {
            return "not_found";
        }
        if (message.contains("无权")) {
            return "restricted";
        }
        return "restricted";
    }

    private String resolveMessage(BusinessException e) {
        String status = resolveStatus(e);
        return switch (status) {
            case "not_found" -> "该订单不存在。";
            case "restricted" -> "当前登录用户无权查看该订单。";
            default -> "当前无法获取该订单详情。";
        };
    }
}
