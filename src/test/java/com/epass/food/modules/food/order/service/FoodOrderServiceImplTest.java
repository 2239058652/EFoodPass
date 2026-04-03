package com.epass.food.modules.food.order.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import com.epass.food.modules.food.order.dto.AppOrderCreateRequest;
import com.epass.food.modules.food.order.dto.AppOrderPayRequest;
import com.epass.food.modules.food.order.dto.AppOrderPreviewResponse;
import com.epass.food.modules.food.order.dto.FoodOrderDetailResponse;
import com.epass.food.modules.food.order.dto.FoodOrderItemRequest;
import com.epass.food.modules.food.order.dto.FoodOrderUpdateStatusRequest;
import com.epass.food.modules.food.order.dto.OrderPaymentStatusCountResponse;
import com.epass.food.modules.food.order.dto.OrderStatusCountResponse;
import com.epass.food.modules.food.order.entity.FoodOrder;
import com.epass.food.modules.food.order.entity.FoodOrderItem;
import com.epass.food.modules.food.order.enums.FoodOrderCloseReason;
import com.epass.food.modules.food.order.enums.FoodOrderPaymentStatus;
import com.epass.food.modules.food.order.enums.FoodOrderStatus;
import com.epass.food.modules.food.order.mapper.FoodOrderItemMapper;
import com.epass.food.modules.food.order.mapper.FoodOrderMapper;
import com.epass.food.modules.food.order.service.impl.FoodOrderServiceImpl;
import com.epass.food.modules.food.stock.service.FoodStockLogService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodOrderServiceImplTest {

    @Mock
    private FoodOrderMapper foodOrderMapper;

    @Mock
    private FoodOrderItemMapper foodOrderItemMapper;

    @Mock
    private FoodItemMapper foodItemMapper;

    @Mock
    private FoodCategoryMapper foodCategoryMapper;

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private FoodStockLogService foodStockLogService;

    @Mock
    private FoodOrderService self;

    private FoodOrderServiceImpl foodOrderService;

    @BeforeEach
    void setUp() {
        foodOrderService = new FoodOrderServiceImpl(
                foodOrderItemMapper,
                foodItemMapper,
                foodCategoryMapper,
                sysUserMapper,
                foodStockLogService,
                self
        );
        ReflectionTestUtils.setField(foodOrderService, "baseMapper", foodOrderMapper);
    }

    @Test
    void previewCurrentUserOrderShouldReturnTotalsWithoutPersisting() {
        when(sysUserMapper.selectById(9L)).thenReturn(activeUser(9L));
        when(foodItemMapper.selectById(101L)).thenReturn(foodItem(101L, 5L, "Milk Tea", "8.50", 10, 1));
        when(foodItemMapper.selectById(102L)).thenReturn(foodItem(102L, 6L, "Coffee", "12.00", 8, 1));
        when(foodCategoryMapper.selectById(5L)).thenReturn(category(5L, 1));
        when(foodCategoryMapper.selectById(6L)).thenReturn(category(6L, 1));

        AppOrderCreateRequest request = new AppOrderCreateRequest();
        request.setRemark("less ice");
        request.setItems(List.of(itemRequest(101L, 2), itemRequest(102L, 1)));

        AppOrderPreviewResponse result = foodOrderService.previewCurrentUserOrder(9L, request);

        assertThat(result.getTotalQuantity()).isEqualTo(3);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("29.00");
        assertThat(result.getRemark()).isEqualTo("less ice");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getFoodName()).isEqualTo("Milk Tea");
        assertThat(result.getItems().get(0).getAmount()).isEqualByComparingTo("17.00");
        verify(foodItemMapper, never()).updateById(any(FoodItem.class));
        verify(foodOrderItemMapper, never()).insert(any(FoodOrderItem.class));
        verify(foodOrderMapper, never()).insert(any(FoodOrder.class));
    }

    @Test
    void payCurrentUserOrderShouldMarkOrderPaid() {
        when(foodOrderMapper.selectById(15L)).thenReturn(order(15L, 9L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode()));

        AppOrderPayRequest request = new AppOrderPayRequest();
        request.setPaymentMethod("alipay");
        foodOrderService.payCurrentUserOrder(9L, 15L, request);

        ArgumentCaptor<FoodOrder> captor = ArgumentCaptor.forClass(FoodOrder.class);
        verify(foodOrderMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPaymentStatus()).isEqualTo(FoodOrderPaymentStatus.PAID.getCode());
        assertThat(captor.getValue().getPaymentMethod()).isEqualTo("ALIPAY");
        assertThat(captor.getValue().getPaidAt()).isNotNull();
    }

    @Test
    void processOrderShouldRejectUnpaidOrder() {
        when(foodOrderMapper.selectById(20L)).thenReturn(order(20L, 7L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode()));

        FoodOrderUpdateStatusRequest request = new FoodOrderUpdateStatusRequest();
        request.setOrderId(20L);

        assertThatThrownBy(() -> foodOrderService.processOrder(request))
                .isInstanceOf(BusinessException.class);

        verify(foodOrderMapper, never()).updateById(any(FoodOrder.class));
    }

    @Test
    void cancelOrderShouldRefundPaidOrderRestoreStockAndWriteDefaultCloseReason() {
        when(foodOrderMapper.selectById(21L)).thenReturn(order(21L, 7L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.PAID.getCode()));
        when(foodOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(21L, 101L, 2)));
        when(foodItemMapper.selectById(101L)).thenReturn(foodItem(101L, 5));

        FoodOrderUpdateStatusRequest request = new FoodOrderUpdateStatusRequest();
        request.setOrderId(21L);
        foodOrderService.cancelOrder(request);

        ArgumentCaptor<FoodItem> itemCaptor = ArgumentCaptor.forClass(FoodItem.class);
        verify(foodItemMapper).updateById(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getStock()).isEqualTo(7);

        ArgumentCaptor<FoodOrder> orderCaptor = ArgumentCaptor.forClass(FoodOrder.class);
        verify(foodOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo(FoodOrderStatus.CANCELED.getCode());
        assertThat(orderCaptor.getValue().getPaymentStatus()).isEqualTo(FoodOrderPaymentStatus.REFUNDED.getCode());
        assertThat(orderCaptor.getValue().getCloseReason()).isEqualTo("MANUAL_CANCEL");
        assertThat(orderCaptor.getValue().getClosedAt()).isNotNull();
        verify(foodStockLogService).recordOrderRestore(101L, 5, 2, 21L);
    }

    @Test
    void cancelCurrentUserOrderShouldUseUserCloseReason() {
        when(foodOrderMapper.selectById(22L)).thenReturn(order(22L, 7L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode()));
        when(foodOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(22L, 101L, 1)));
        when(foodItemMapper.selectById(101L)).thenReturn(foodItem(101L, 5));

        foodOrderService.cancelCurrentUserOrder(7L, 22L);

        ArgumentCaptor<FoodOrder> orderCaptor = ArgumentCaptor.forClass(FoodOrder.class);
        verify(foodOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getCloseReason()).isEqualTo("USER_CANCEL");
        assertThat(orderCaptor.getValue().getClosedAt()).isNotNull();
    }

    @Test
    void refundCompletedPaidOrderShouldOnlyMarkRefunded() {
        when(foodOrderMapper.selectById(30L)).thenReturn(order(30L, 7L, FoodOrderStatus.COMPLETED.getCode(), FoodOrderPaymentStatus.PAID.getCode()));

        FoodOrderUpdateStatusRequest request = new FoodOrderUpdateStatusRequest();
        request.setOrderId(30L);
        foodOrderService.refundOrder(request);

        ArgumentCaptor<FoodOrder> orderCaptor = ArgumentCaptor.forClass(FoodOrder.class);
        verify(foodOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo(FoodOrderStatus.COMPLETED.getCode());
        assertThat(orderCaptor.getValue().getPaymentStatus()).isEqualTo(FoodOrderPaymentStatus.REFUNDED.getCode());
        assertThat(orderCaptor.getValue().getCloseReason()).isNull();
        verify(foodOrderItemMapper, never()).selectList(any());
    }

    @Test
    void refundPendingPaidOrderShouldCancelWithRefundCloseReason() {
        when(foodOrderMapper.selectById(31L)).thenReturn(order(31L, 7L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.PAID.getCode()));
        when(foodOrderItemMapper.selectList(any())).thenReturn(List.of(orderItem(31L, 101L, 1)));
        when(foodItemMapper.selectById(101L)).thenReturn(foodItem(101L, 3));

        FoodOrderUpdateStatusRequest request = new FoodOrderUpdateStatusRequest();
        request.setOrderId(31L);
        foodOrderService.refundOrder(request);

        ArgumentCaptor<FoodOrder> orderCaptor = ArgumentCaptor.forClass(FoodOrder.class);
        verify(foodOrderMapper).updateById(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderStatus()).isEqualTo(FoodOrderStatus.CANCELED.getCode());
        assertThat(orderCaptor.getValue().getPaymentStatus()).isEqualTo(FoodOrderPaymentStatus.REFUNDED.getCode());
        assertThat(orderCaptor.getValue().getCloseReason()).isEqualTo("REFUND_CANCEL");
        assertThat(orderCaptor.getValue().getClosedAt()).isNotNull();
    }

    @Test
    void refundShouldRejectUnpaidOrder() {
        when(foodOrderMapper.selectById(32L)).thenReturn(order(32L, 7L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode()));

        FoodOrderUpdateStatusRequest request = new FoodOrderUpdateStatusRequest();
        request.setOrderId(32L);

        assertThatThrownBy(() -> foodOrderService.refundOrder(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getOrderDetailShouldExposeLabelsAndCloseInfo() {
        FoodOrder order = order(40L, 7L, FoodOrderStatus.CANCELED.getCode(), FoodOrderPaymentStatus.UNPAID.getCode());
        LocalDateTime closedAt = LocalDateTime.of(2026, 4, 3, 10, 30);
        order.setCloseReason("PAYMENT_TIMEOUT_AUTO_CANCEL");
        order.setClosedAt(closedAt);
        when(foodOrderMapper.selectById(40L)).thenReturn(order);

        FoodOrderItem detailItem = new FoodOrderItem();
        detailItem.setOrderId(40L);
        detailItem.setFoodItemId(101L);
        detailItem.setFoodNameSnapshot("Milk Tea");
        detailItem.setPriceSnapshot(new BigDecimal("8.50"));
        detailItem.setQuantity(2);
        detailItem.setAmount(new BigDecimal("17.00"));
        when(foodOrderItemMapper.selectList(any())).thenReturn(List.of(detailItem));

        FoodOrderDetailResponse result = foodOrderService.getOrderDetail(40L);

        assertThat(result.getOrderStatusLabel()).isEqualTo(FoodOrderStatus.CANCELED.getLabel());
        assertThat(result.getPaymentStatusLabel()).isEqualTo(FoodOrderPaymentStatus.UNPAID.getLabel());
        assertThat(result.getCloseReason()).isEqualTo("PAYMENT_TIMEOUT_AUTO_CANCEL");
        assertThat(result.getCloseReasonLabel()).isEqualTo(FoodOrderCloseReason.PAYMENT_TIMEOUT_AUTO_CANCEL.getLabel());
        assertThat(result.getClosedAt()).isEqualTo(closedAt);
        assertThat(result.getItems()).hasSize(1);
    }

    @Test
    void getOrderPaymentStatusCountsShouldCountAllStatusesAndPopulateLabels() {
        when(foodOrderMapper.selectList(any())).thenReturn(List.of(
                order(50L, 1L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode()),
                order(51L, 1L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.PAID.getCode()),
                order(52L, 1L, FoodOrderStatus.CANCELED.getCode(), FoodOrderPaymentStatus.REFUNDED.getCode()),
                order(53L, 1L, FoodOrderStatus.PENDING.getCode(), null)
        ));

        List<OrderPaymentStatusCountResponse> result = foodOrderService.getOrderPaymentStatusCounts();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPaymentStatus()).isEqualTo(FoodOrderPaymentStatus.UNPAID.getCode());
        assertThat(result.get(0).getPaymentStatusLabel()).isEqualTo(FoodOrderPaymentStatus.UNPAID.getLabel());
        assertThat(result.get(0).getOrderCount()).isEqualTo(2L);
        assertThat(result.get(1).getPaymentStatus()).isEqualTo(FoodOrderPaymentStatus.PAID.getCode());
        assertThat(result.get(1).getPaymentStatusLabel()).isEqualTo(FoodOrderPaymentStatus.PAID.getLabel());
        assertThat(result.get(1).getOrderCount()).isEqualTo(1L);
        assertThat(result.get(2).getPaymentStatus()).isEqualTo(FoodOrderPaymentStatus.REFUNDED.getCode());
        assertThat(result.get(2).getPaymentStatusLabel()).isEqualTo(FoodOrderPaymentStatus.REFUNDED.getLabel());
        assertThat(result.get(2).getOrderCount()).isEqualTo(1L);
    }

    @Test
    void getOrderStatusCountsShouldPopulateLabels() {
        when(foodOrderMapper.selectList(any())).thenReturn(List.of(
                order(60L, 1L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode()),
                order(61L, 1L, FoodOrderStatus.PROCESSING.getCode(), FoodOrderPaymentStatus.PAID.getCode()),
                order(62L, 1L, FoodOrderStatus.COMPLETED.getCode(), FoodOrderPaymentStatus.PAID.getCode()),
                order(63L, 1L, FoodOrderStatus.CANCELED.getCode(), FoodOrderPaymentStatus.REFUNDED.getCode())
        ));

        List<OrderStatusCountResponse> result = foodOrderService.getOrderStatusCounts();

        assertThat(result).hasSize(4);
        assertThat(result.get(0).getOrderStatus()).isEqualTo(FoodOrderStatus.PENDING.getCode());
        assertThat(result.get(0).getOrderStatusLabel()).isEqualTo(FoodOrderStatus.PENDING.getLabel());
        assertThat(result.get(0).getOrderCount()).isEqualTo(1L);
        assertThat(result.get(3).getOrderStatus()).isEqualTo(FoodOrderStatus.CANCELED.getCode());
        assertThat(result.get(3).getOrderStatusLabel()).isEqualTo(FoodOrderStatus.CANCELED.getLabel());
        assertThat(result.get(3).getOrderCount()).isEqualTo(1L);
    }

    @Test
    void closeExpiredUnpaidOrdersShouldDelegateCancelThroughProxyWithAutoCloseReason() {
        when(foodOrderMapper.selectList(any())).thenReturn(List.of(
                order(70L, 1L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode()),
                order(71L, 1L, FoodOrderStatus.PENDING.getCode(), FoodOrderPaymentStatus.UNPAID.getCode())
        ));

        int closedCount = foodOrderService.closeExpiredUnpaidOrders(15, 20);

        assertThat(closedCount).isEqualTo(2);
        verify(self).cancelOrder(argThat(request ->
                request != null
                        && Long.valueOf(70L).equals(request.getOrderId())
                        && "PAYMENT_TIMEOUT_AUTO_CANCEL".equals(request.getCloseReason())
        ));
        verify(self).cancelOrder(argThat(request ->
                request != null
                        && Long.valueOf(71L).equals(request.getOrderId())
                        && "PAYMENT_TIMEOUT_AUTO_CANCEL".equals(request.getCloseReason())
        ));
    }

    @Test
    void exportOrdersShouldReturnCsvWithLabels() {
        FoodOrder order = order(80L, 3L, FoodOrderStatus.CANCELED.getCode(), FoodOrderPaymentStatus.REFUNDED.getCode());
        order.setPaymentMethod("ALIPAY");
        order.setCloseReason("REFUND_CANCEL");
        order.setRemark("less sugar");
        order.setCreatedAt(LocalDateTime.of(2026, 4, 3, 12, 0));
        order.setPaidAt(LocalDateTime.of(2026, 4, 3, 12, 5));
        order.setClosedAt(LocalDateTime.of(2026, 4, 3, 12, 10));
        when(foodOrderMapper.selectList(any())).thenReturn(List.of(order));

        byte[] bytes = foodOrderService.exportOrders(new com.epass.food.modules.food.order.dto.FoodOrderListQuery());
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("orderNo,userId,totalAmount");
        assertThat(csv).contains("NO-80");
        assertThat(csv).contains(FoodOrderStatus.CANCELED.getLabel());
        assertThat(csv).contains(FoodOrderPaymentStatus.REFUNDED.getLabel());
        assertThat(csv).contains(FoodOrderCloseReason.REFUND_CANCEL.getLabel());
        assertThat(csv).contains("less sugar");
    }

    private FoodOrder order(Long id, Long userId, Integer orderStatus, Integer paymentStatus) {
        FoodOrder order = new FoodOrder();
        order.setId(id);
        order.setUserId(userId);
        order.setOrderNo("NO-" + id);
        order.setTotalAmount(new BigDecimal("20.00"));
        order.setOrderStatus(orderStatus);
        order.setPaymentStatus(paymentStatus);
        return order;
    }

    private FoodOrderItem orderItem(Long orderId, Long foodItemId, Integer quantity) {
        FoodOrderItem orderItem = new FoodOrderItem();
        orderItem.setOrderId(orderId);
        orderItem.setFoodItemId(foodItemId);
        orderItem.setQuantity(quantity);
        return orderItem;
    }

    private FoodOrderItemRequest itemRequest(Long foodItemId, Integer quantity) {
        FoodOrderItemRequest request = new FoodOrderItemRequest();
        request.setFoodItemId(foodItemId);
        request.setQuantity(quantity);
        return request;
    }

    private FoodItem foodItem(Long id, Integer stock) {
        return foodItem(id, 5L, "Item-" + id, "10.00", stock, 1);
    }

    private FoodItem foodItem(Long id,
                              Long categoryId,
                              String name,
                              String price,
                              Integer stock,
                              Integer isOnSale) {
        FoodItem item = new FoodItem();
        item.setId(id);
        item.setCategoryId(categoryId);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setStock(stock);
        item.setIsOnSale(isOnSale);
        return item;
    }

    private FoodCategory category(Long id, Integer status) {
        FoodCategory category = new FoodCategory();
        category.setId(id);
        category.setStatus(status);
        return category;
    }

    private SysUser activeUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(1);
        return user;
    }
}
