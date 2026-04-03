package com.epass.food.modules.food.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.order.dto.AppOrderCreateRequest;
import com.epass.food.modules.food.order.dto.AppOrderPayRequest;
import com.epass.food.modules.food.order.dto.AppOrderPreviewResponse;
import com.epass.food.modules.food.order.dto.FoodOrderCreateRequest;
import com.epass.food.modules.food.order.dto.FoodOrderDetailResponse;
import com.epass.food.modules.food.order.dto.FoodOrderListQuery;
import com.epass.food.modules.food.order.dto.FoodOrderListResponse;
import com.epass.food.modules.food.order.dto.FoodOrderUpdateStatusRequest;
import com.epass.food.modules.food.order.dto.OrderDailyAmountResponse;
import com.epass.food.modules.food.order.dto.OrderPaymentStatusCountResponse;
import com.epass.food.modules.food.order.dto.OrderStatQuery;
import com.epass.food.modules.food.order.dto.OrderStatOverviewResponse;
import com.epass.food.modules.food.order.dto.OrderStatusCountResponse;
import com.epass.food.modules.food.order.dto.OrderTopItemResponse;
import com.epass.food.modules.food.order.entity.FoodOrder;

import java.util.List;

public interface FoodOrderService extends IService<FoodOrder> {

    PageResult<FoodOrderListResponse> listOrders(FoodOrderListQuery query);

    byte[] exportOrders(FoodOrderListQuery query);

    FoodOrderDetailResponse getOrderDetail(Long orderId);

    void createOrder(FoodOrderCreateRequest request);

    void processOrder(FoodOrderUpdateStatusRequest request);

    void cancelOrder(FoodOrderUpdateStatusRequest request);

    void refundOrder(FoodOrderUpdateStatusRequest request);

    void completeOrder(FoodOrderUpdateStatusRequest request);

    PageResult<FoodOrderListResponse> listCurrentUserOrders(Long userId, FoodOrderListQuery query);

    FoodOrderDetailResponse getCurrentUserOrderDetail(Long userId, Long orderId);

    AppOrderPreviewResponse previewCurrentUserOrder(Long userId, AppOrderCreateRequest request);

    void createCurrentUserOrder(Long userId, AppOrderCreateRequest request);

    void payCurrentUserOrder(Long userId, Long orderId, AppOrderPayRequest request);

    void cancelCurrentUserOrder(Long userId, Long orderId);

    int closeExpiredUnpaidOrders(int timeoutMinutes, int batchSize);

    OrderStatOverviewResponse getOrderStatOverview();

    OrderStatOverviewResponse getOrderStatOverview(OrderStatQuery query);

    List<OrderStatusCountResponse> getOrderStatusCounts();

    List<OrderStatusCountResponse> getOrderStatusCounts(OrderStatQuery query);

    List<OrderPaymentStatusCountResponse> getOrderPaymentStatusCounts();

    List<OrderPaymentStatusCountResponse> getOrderPaymentStatusCounts(OrderStatQuery query);

    List<OrderTopItemResponse> getTopSellingItems();

    List<OrderTopItemResponse> getTopSellingItems(OrderStatQuery query);

    List<OrderDailyAmountResponse> getDailyAmounts();

    List<OrderDailyAmountResponse> getDailyAmounts(OrderStatQuery query);
}
