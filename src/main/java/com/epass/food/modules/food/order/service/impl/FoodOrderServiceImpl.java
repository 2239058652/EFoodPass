package com.epass.food.modules.food.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import com.epass.food.modules.food.order.dto.*;
import com.epass.food.modules.food.order.enums.FoodOrderCloseReason;
import com.epass.food.modules.food.order.enums.FoodOrderPaymentStatus;
import com.epass.food.modules.food.order.entity.FoodOrder;
import com.epass.food.modules.food.order.entity.FoodOrderItem;
import com.epass.food.modules.food.order.enums.FoodOrderStatus;
import com.epass.food.modules.food.order.mapper.FoodOrderItemMapper;
import com.epass.food.modules.food.order.mapper.FoodOrderMapper;
import com.epass.food.modules.food.order.service.FoodOrderService;
import com.epass.food.modules.food.stock.service.FoodStockLogService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FoodOrderServiceImpl extends ServiceImpl<FoodOrderMapper, FoodOrder> implements FoodOrderService {

    private static final Logger log = LoggerFactory.getLogger(FoodOrderServiceImpl.class);
    private static final List<String> SUPPORTED_PAYMENT_METHODS = List.of("MOCK", "ALIPAY", "WECHAT");
    private static final String CLOSE_REASON_ADMIN_CANCEL = FoodOrderCloseReason.MANUAL_CANCEL.getCode();
    private static final String CLOSE_REASON_USER_CANCEL = FoodOrderCloseReason.USER_CANCEL.getCode();
    private static final String CLOSE_REASON_AUTO_CLOSE = FoodOrderCloseReason.PAYMENT_TIMEOUT_AUTO_CANCEL.getCode();
    private static final String CLOSE_REASON_REFUND_CANCEL = FoodOrderCloseReason.REFUND_CANCEL.getCode();

    private final FoodOrderItemMapper foodOrderItemMapper;
    private final FoodItemMapper foodItemMapper;
    private final FoodCategoryMapper foodCategoryMapper;
    private final SysUserMapper sysUserMapper;
    private final FoodStockLogService foodStockLogService;
    private final FoodOrderService self;

    private static class PreparedOrderItem {

        private final FoodItem foodItem;
        private final Integer quantity;
        private final BigDecimal amount;

        private PreparedOrderItem(FoodItem foodItem, Integer quantity, BigDecimal amount) {
            this.foodItem = foodItem;
            this.quantity = quantity;
            this.amount = amount;
        }
    }

    private static class OrderDraft {

        private final BigDecimal totalAmount;
        private final Integer totalQuantity;
        private final List<PreparedOrderItem> items;
        private final Map<Long, Integer> itemQuantityMap;
        private final Collection<FoodItem> itemsToUpdate;

        private OrderDraft(BigDecimal totalAmount,
                           Integer totalQuantity,
                           List<PreparedOrderItem> items,
                           Map<Long, Integer> itemQuantityMap,
                           Collection<FoodItem> itemsToUpdate) {
            this.totalAmount = totalAmount;
            this.totalQuantity = totalQuantity;
            this.items = items;
            this.itemQuantityMap = itemQuantityMap;
            this.itemsToUpdate = itemsToUpdate;
        }
    }

    public FoodOrderServiceImpl(FoodOrderItemMapper foodOrderItemMapper,
                                FoodItemMapper foodItemMapper,
                                FoodCategoryMapper foodCategoryMapper,
                                SysUserMapper sysUserMapper,
                                FoodStockLogService foodStockLogService,
                                @Lazy FoodOrderService self) {
        this.foodOrderItemMapper = foodOrderItemMapper;
        this.foodItemMapper = foodItemMapper;
        this.foodCategoryMapper = foodCategoryMapper;
        this.sysUserMapper = sysUserMapper;
        this.foodStockLogService = foodStockLogService;
        this.self = self;
    }

    private String normalizeCloseReason(String closeReason, String defaultReason) {
        if (StringUtils.hasText(closeReason)) {
            return closeReason.trim();
        }
        return defaultReason;
    }

    private void fillOrderListPresentation(FoodOrderListResponse response, FoodOrder order) {
        response.setOrderStatusLabel(FoodOrderStatus.getLabelByCode(order.getOrderStatus()));
        response.setPaymentStatusLabel(FoodOrderPaymentStatus.getLabelByCode(normalizePaymentStatus(order.getPaymentStatus())));
        response.setCloseReason(order.getCloseReason());
        response.setCloseReasonLabel(FoodOrderCloseReason.getLabelByCode(order.getCloseReason()));
        response.setClosedAt(order.getClosedAt());
    }

    private void fillOrderDetailPresentation(FoodOrderDetailResponse response, FoodOrder order) {
        response.setOrderStatusLabel(FoodOrderStatus.getLabelByCode(order.getOrderStatus()));
        response.setPaymentStatusLabel(FoodOrderPaymentStatus.getLabelByCode(normalizePaymentStatus(order.getPaymentStatus())));
        response.setCloseReason(order.getCloseReason());
        response.setCloseReasonLabel(FoodOrderCloseReason.getLabelByCode(order.getCloseReason()));
        response.setClosedAt(order.getClosedAt());
    }

    private OrderDraft prepareOrderDraft(List<FoodOrderItemRequest> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new BusinessException(BizErrorCode.ORDER_ITEMS_EMPTY, "Order items cannot be empty");
        }

        Map<Long, Integer> itemQuantityMap = new LinkedHashMap<>();
        for (FoodOrderItemRequest itemRequest : requestedItems) {
            itemQuantityMap.merge(itemRequest.getFoodItemId(), itemRequest.getQuantity(), Integer::sum);
        }

        Map<Long, FoodItem> itemMap = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : itemQuantityMap.entrySet()) {
            Long foodItemId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            FoodItem item = foodItemMapper.selectById(foodItemId);
            if (item == null) {
                throw new BusinessException(BizErrorCode.ORDER_ITEM_NOT_FOUND, "Food item not found");
            }

            if (!Integer.valueOf(1).equals(item.getIsOnSale())) {
                throw new BusinessException(BizErrorCode.ORDER_ITEM_NOT_ON_SALE, "Food item is not on sale");
            }

            FoodCategory category = foodCategoryMapper.selectById(item.getCategoryId());
            if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
                throw new BusinessException(BizErrorCode.ORDER_ITEM_CATEGORY_INVALID, "Food item category is unavailable");
            }

            if (item.getStock() == null || item.getStock() < totalQuantity) {
                throw new BusinessException(BizErrorCode.ORDER_ITEM_STOCK_NOT_ENOUGH, "Food item stock is not enough");
            }

            item.setStock(item.getStock() - totalQuantity);
            itemMap.put(foodItemId, item);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<PreparedOrderItem> preparedItems = new ArrayList<>();
        for (FoodOrderItemRequest itemRequest : requestedItems) {
            FoodItem item = itemMap.get(itemRequest.getFoodItemId());
            BigDecimal itemAmount = item.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);
            totalQuantity += itemRequest.getQuantity();
            preparedItems.add(new PreparedOrderItem(item, itemRequest.getQuantity(), itemAmount));
        }

        return new OrderDraft(totalAmount, totalQuantity, preparedItems, itemQuantityMap, itemMap.values());
    }

    private AppOrderPreviewResponse buildPreviewResponse(OrderDraft orderDraft, String remark) {
        List<AppOrderPreviewItemResponse> itemResponses = new ArrayList<>();
        for (PreparedOrderItem preparedItem : orderDraft.items) {
            AppOrderPreviewItemResponse itemResponse = new AppOrderPreviewItemResponse();
            itemResponse.setFoodItemId(preparedItem.foodItem.getId());
            itemResponse.setFoodName(preparedItem.foodItem.getName());
            itemResponse.setPrice(preparedItem.foodItem.getPrice());
            itemResponse.setQuantity(preparedItem.quantity);
            itemResponse.setAmount(preparedItem.amount);
            itemResponses.add(itemResponse);
        }

        AppOrderPreviewResponse response = new AppOrderPreviewResponse();
        response.setTotalQuantity(orderDraft.totalQuantity);
        response.setTotalAmount(orderDraft.totalAmount);
        response.setRemark(remark);
        response.setItems(itemResponses);
        return response;
    }

    private void validateOrderStatus(Integer orderStatus) {
        if (!FoodOrderStatus.isValid(orderStatus)) {
            throw new BusinessException(BizErrorCode.ORDER_STATUS_INVALID, "订单状态值不合法");
        }
    }

    private void validatePaymentStatus(Integer paymentStatus) {
        if (!FoodOrderPaymentStatus.isValid(paymentStatus)) {
            throw new BusinessException(BizErrorCode.ORDER_STATUS_INVALID, "支付状态值不合法");
        }
    }

    private Integer normalizePaymentStatus(Integer paymentStatus) {
        return paymentStatus == null ? FoodOrderPaymentStatus.UNPAID.getCode() : paymentStatus;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (!StringUtils.hasText(paymentMethod)) {
            throw new BusinessException(BizErrorCode.ORDER_PAYMENT_METHOD_INVALID, "支付方式不能为空");
        }

        String normalized = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_PAYMENT_METHODS.contains(normalized)) {
            throw new BusinessException(BizErrorCode.ORDER_PAYMENT_METHOD_INVALID, "支付方式不支持");
        }
        return normalized;
    }

    private FoodOrder getRequiredOrder(Long orderId) {
        FoodOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(BizErrorCode.ORDER_NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(BizErrorCode.ORDER_USER_NOT_FOUND, "下单用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(BizErrorCode.ORDER_USER_DISABLED, "下单用户已被禁用");
        }
        return user;
    }

    private String generateOrderNo() {
        String timePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomPart = ThreadLocalRandom.current().nextInt(100000, 999999);
        return timePart + randomPart;
    }

    private FoodOrderListResponse buildListResponse(FoodOrder order) {
        FoodOrderListResponse response = new FoodOrderListResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(order.getOrderStatus());
        response.setPaymentStatus(normalizePaymentStatus(order.getPaymentStatus()));
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaidAt(order.getPaidAt());
        response.setRemark(order.getRemark());
        response.setCreatedAt(order.getCreatedAt());
        fillOrderListPresentation(response, order);
        return response;
    }

    private LambdaQueryWrapper<FoodOrder> buildOrderQueryWrapper(FoodOrderListQuery query) {
        LambdaQueryWrapper<FoodOrder> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getOrderNo())) {
            queryWrapper.like(FoodOrder::getOrderNo, query.getOrderNo().trim());
        }

        if (query.getUserId() != null) {
            queryWrapper.eq(FoodOrder::getUserId, query.getUserId());
        }

        if (query.getOrderStatus() != null) {
            validateOrderStatus(query.getOrderStatus());
            queryWrapper.eq(FoodOrder::getOrderStatus, query.getOrderStatus());
        }

        if (query.getPaymentStatus() != null) {
            validatePaymentStatus(query.getPaymentStatus());
            queryWrapper.eq(FoodOrder::getPaymentStatus, query.getPaymentStatus());
        }

        if (query.getCreatedAtStart() != null) {
            queryWrapper.ge(FoodOrder::getCreatedAt, query.getCreatedAtStart());
        }

        if (query.getCreatedAtEnd() != null) {
            queryWrapper.le(FoodOrder::getCreatedAt, query.getCreatedAtEnd());
        }

        queryWrapper.orderByDesc(FoodOrder::getId);
        return queryWrapper;
    }

    private List<FoodOrderListResponse> buildListResponses(List<FoodOrder> orders) {
        List<FoodOrderListResponse> responseList = new ArrayList<>();
        for (FoodOrder order : orders) {
            responseList.add(buildListResponse(order));
        }
        return responseList;
    }

    private LambdaQueryWrapper<FoodOrder> buildStatOrderQueryWrapper(OrderStatQuery query) {
        LambdaQueryWrapper<FoodOrder> queryWrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getCreatedAtStart() != null) {
                queryWrapper.ge(FoodOrder::getCreatedAt, query.getCreatedAtStart());
            }
            if (query.getCreatedAtEnd() != null) {
                queryWrapper.le(FoodOrder::getCreatedAt, query.getCreatedAtEnd());
            }
        }
        return queryWrapper;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private FoodOrder getRequiredUserOwnedOrder(Long userId, Long orderId) {
        FoodOrder order = getRequiredOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(BizErrorCode.ORDER_NO_PERMISSION, "无权操作该订单");
        }
        return order;
    }

    @Override
    public PageResult<FoodOrderListResponse> listOrders(FoodOrderListQuery query) {
        if (query == null) {
            query = new FoodOrderListQuery();
        }
        LambdaQueryWrapper<FoodOrder> queryWrapper = buildOrderQueryWrapper(query);

        Page<FoodOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<FoodOrder> orderPage = this.page(page, queryWrapper);

        PageResult<FoodOrderListResponse> result = new PageResult<>();
        result.setTotal(orderPage.getTotal());
        result.setPageNum(orderPage.getCurrent());
        result.setPageSize(orderPage.getSize());
        result.setRecords(buildListResponses(orderPage.getRecords()));
        return result;
    }

    @Override
    public byte[] exportOrders(FoodOrderListQuery query) {
        if (query == null) {
            query = new FoodOrderListQuery();
        }

        List<FoodOrderListResponse> responses = buildListResponses(this.list(buildOrderQueryWrapper(query)));
        StringBuilder builder = new StringBuilder();
        builder.append('\uFEFF');
        builder.append("orderNo,userId,totalAmount,orderStatus,orderStatusLabel,paymentStatus,paymentStatusLabel,paymentMethod,closeReason,closeReasonLabel,remark,paidAt,closedAt,createdAt")
                .append('\n');

        for (FoodOrderListResponse response : responses) {
            builder.append(escapeCsv(response.getOrderNo())).append(',')
                    .append(response.getUserId() == null ? "" : response.getUserId()).append(',')
                    .append(response.getTotalAmount() == null ? "" : response.getTotalAmount().toPlainString()).append(',')
                    .append(response.getOrderStatus() == null ? "" : response.getOrderStatus()).append(',')
                    .append(escapeCsv(response.getOrderStatusLabel())).append(',')
                    .append(response.getPaymentStatus() == null ? "" : response.getPaymentStatus()).append(',')
                    .append(escapeCsv(response.getPaymentStatusLabel())).append(',')
                    .append(escapeCsv(response.getPaymentMethod())).append(',')
                    .append(escapeCsv(response.getCloseReason())).append(',')
                    .append(escapeCsv(response.getCloseReasonLabel())).append(',')
                    .append(escapeCsv(response.getRemark())).append(',')
                    .append(escapeCsv(formatDateTime(response.getPaidAt()))).append(',')
                    .append(escapeCsv(formatDateTime(response.getClosedAt()))).append(',')
                    .append(escapeCsv(formatDateTime(response.getCreatedAt()))).append('\n');
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public FoodOrderDetailResponse getOrderDetail(Long orderId) {
        FoodOrder order = getRequiredOrder(orderId);

        List<FoodOrderItem> orderItemList = foodOrderItemMapper.selectList(
                new LambdaQueryWrapper<FoodOrderItem>()
                        .eq(FoodOrderItem::getOrderId, orderId)
                        .orderByAsc(FoodOrderItem::getId)
        );

        List<FoodOrderItemResponse> itemResponses = new ArrayList<>();
        for (FoodOrderItem orderItem : orderItemList) {
            FoodOrderItemResponse itemResponse = new FoodOrderItemResponse();
            itemResponse.setFoodItemId(orderItem.getFoodItemId());
            itemResponse.setFoodNameSnapshot(orderItem.getFoodNameSnapshot());
            itemResponse.setPriceSnapshot(orderItem.getPriceSnapshot());
            itemResponse.setQuantity(orderItem.getQuantity());
            itemResponse.setAmount(orderItem.getAmount());
            itemResponses.add(itemResponse);
        }

        FoodOrderDetailResponse response = new FoodOrderDetailResponse();
        response.setId(order.getId());
        response.setOrderNo(order.getOrderNo());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setOrderStatus(order.getOrderStatus());
        response.setPaymentStatus(normalizePaymentStatus(order.getPaymentStatus()));
        response.setPaymentMethod(order.getPaymentMethod());
        response.setPaidAt(order.getPaidAt());
        response.setRemark(order.getRemark());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(itemResponses);
        fillOrderDetailPresentation(response, order);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(FoodOrderCreateRequest request) {
        getRequiredUser(request.getUserId());
        OrderDraft orderDraft = prepareOrderDraft(request.getItems());

        FoodOrder order = new FoodOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setTotalAmount(orderDraft.totalAmount);
        order.setOrderStatus(FoodOrderStatus.PENDING.getCode());
        order.setPaymentStatus(FoodOrderPaymentStatus.UNPAID.getCode());
        order.setRemark(request.getRemark());

        this.save(order);

        for (PreparedOrderItem preparedItem : orderDraft.items) {
            FoodOrderItem orderItem = new FoodOrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setFoodItemId(preparedItem.foodItem.getId());
            orderItem.setFoodNameSnapshot(preparedItem.foodItem.getName());
            orderItem.setPriceSnapshot(preparedItem.foodItem.getPrice());
            orderItem.setQuantity(preparedItem.quantity);
            orderItem.setAmount(preparedItem.amount);
            foodOrderItemMapper.insert(orderItem);
        }

        for (FoodItem item : orderDraft.itemsToUpdate) {
            int afterStock = item.getStock();
            int beforeStock = afterStock + orderDraft.itemQuantityMap.get(item.getId());
            foodItemMapper.updateById(item);
            foodStockLogService.recordOrderDeduct(
                    item.getId(),
                    beforeStock,
                    orderDraft.itemQuantityMap.get(item.getId()),
                    order.getId()
            );
        }
    }

    @Override
    public void processOrder(FoodOrderUpdateStatusRequest request) {
        FoodOrder order = getRequiredOrder(request.getOrderId());

        if (!Integer.valueOf(FoodOrderStatus.PENDING.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(BizErrorCode.ORDER_ONLY_PENDING_CAN_PROCESS, "只有待确认订单才能开始制作");
        }

        if (!Integer.valueOf(FoodOrderPaymentStatus.PAID.getCode()).equals(normalizePaymentStatus(order.getPaymentStatus()))) {
            throw new BusinessException(BizErrorCode.ORDER_UNPAID_CANNOT_PROCESS, "订单未支付，不能开始制作");
        }

        order.setOrderStatus(FoodOrderStatus.PROCESSING.getCode());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(FoodOrderUpdateStatusRequest request) {
        FoodOrder order = getRequiredOrder(request.getOrderId());

        if (Integer.valueOf(FoodOrderStatus.COMPLETED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(BizErrorCode.ORDER_COMPLETED_CANNOT_CANCEL, "已完成订单不能取消");
        }

        if (Integer.valueOf(FoodOrderStatus.CANCELED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(BizErrorCode.ORDER_ALREADY_CANCELED, "订单已取消，请勿重复操作");
        }

        List<FoodOrderItem> orderItemList = foodOrderItemMapper.selectList(
                new LambdaQueryWrapper<FoodOrderItem>()
                        .eq(FoodOrderItem::getOrderId, request.getOrderId())
        );

        for (FoodOrderItem orderItem : orderItemList) {
            FoodItem item = foodItemMapper.selectById(orderItem.getFoodItemId());
            if (item == null) {
                throw new BusinessException(BizErrorCode.ORDER_RESTORE_ITEM_NOT_FOUND, "订单关联菜品不存在，无法回补库存");
            }

            int beforeStock = item.getStock() == null ? 0 : item.getStock();
            item.setStock(beforeStock + orderItem.getQuantity());
            foodItemMapper.updateById(item);
            foodStockLogService.recordOrderRestore(
                    item.getId(),
                    beforeStock,
                    orderItem.getQuantity(),
                    order.getId()
            );
        }

        order.setOrderStatus(FoodOrderStatus.CANCELED.getCode());
        if (Integer.valueOf(FoodOrderPaymentStatus.PAID.getCode()).equals(normalizePaymentStatus(order.getPaymentStatus()))) {
            order.setPaymentStatus(FoodOrderPaymentStatus.REFUNDED.getCode());
        }
        order.setCloseReason(normalizeCloseReason(request.getCloseReason(), CLOSE_REASON_ADMIN_CANCEL));
        order.setClosedAt(LocalDateTime.now());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(FoodOrderUpdateStatusRequest request) {
        FoodOrder order = getRequiredOrder(request.getOrderId());
        Integer paymentStatus = normalizePaymentStatus(order.getPaymentStatus());

        if (Integer.valueOf(FoodOrderPaymentStatus.REFUNDED.getCode()).equals(paymentStatus)) {
            throw new BusinessException(BizErrorCode.ORDER_ALREADY_REFUNDED, "订单已退款，请勿重复操作");
        }
        if (!Integer.valueOf(FoodOrderPaymentStatus.PAID.getCode()).equals(paymentStatus)) {
            throw new BusinessException(BizErrorCode.ORDER_ONLY_PAID_CAN_REFUND, "只有已支付订单才能退款");
        }

        if (Integer.valueOf(FoodOrderStatus.PENDING.getCode()).equals(order.getOrderStatus())
                || Integer.valueOf(FoodOrderStatus.PROCESSING.getCode()).equals(order.getOrderStatus())) {
            request.setCloseReason(normalizeCloseReason(request.getCloseReason(), CLOSE_REASON_REFUND_CANCEL));
            cancelOrder(request);
            return;
        }

        order.setPaymentStatus(FoodOrderPaymentStatus.REFUNDED.getCode());
        this.updateById(order);
    }

    @Override
    public void completeOrder(FoodOrderUpdateStatusRequest request) {
        FoodOrder order = getRequiredOrder(request.getOrderId());

        if (!Integer.valueOf(FoodOrderStatus.PROCESSING.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(BizErrorCode.ORDER_ONLY_PROCESSING_CAN_COMPLETE, "只有制作中的订单才能完成");
        }

        order.setOrderStatus(FoodOrderStatus.COMPLETED.getCode());
        this.updateById(order);
    }

    @Override
    public PageResult<FoodOrderListResponse> listCurrentUserOrders(Long userId, FoodOrderListQuery query) {
        if (query == null) {
            query = new FoodOrderListQuery();
        }

        query.setUserId(userId);
        return listOrders(query);
    }

    @Override
    public FoodOrderDetailResponse getCurrentUserOrderDetail(Long userId, Long orderId) {
        getRequiredUserOwnedOrder(userId, orderId);
        return getOrderDetail(orderId);
    }

    @Override
    public AppOrderPreviewResponse previewCurrentUserOrder(Long userId, AppOrderCreateRequest request) {
        getRequiredUser(userId);
        OrderDraft orderDraft = prepareOrderDraft(request.getItems());
        return buildPreviewResponse(orderDraft, request.getRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCurrentUserOrder(Long userId, AppOrderCreateRequest request) {
        FoodOrderCreateRequest createRequest = new FoodOrderCreateRequest();
        createRequest.setUserId(userId);
        createRequest.setRemark(request.getRemark());
        createRequest.setItems(request.getItems());
        createOrder(createRequest);
    }

    @Override
    public void payCurrentUserOrder(Long userId, Long orderId, AppOrderPayRequest request) {
        FoodOrder order = getRequiredUserOwnedOrder(userId, orderId);

        if (Integer.valueOf(FoodOrderStatus.CANCELED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(BizErrorCode.ORDER_CANNOT_PAY, "已取消订单不能支付");
        }
        if (Integer.valueOf(FoodOrderStatus.COMPLETED.getCode()).equals(order.getOrderStatus())) {
            throw new BusinessException(BizErrorCode.ORDER_CANNOT_PAY, "已完成订单不能支付");
        }
        if (Integer.valueOf(FoodOrderPaymentStatus.PAID.getCode()).equals(normalizePaymentStatus(order.getPaymentStatus()))) {
            throw new BusinessException(BizErrorCode.ORDER_ALREADY_PAID, "订单已支付，请勿重复操作");
        }

        order.setPaymentStatus(FoodOrderPaymentStatus.PAID.getCode());
        order.setPaymentMethod(normalizePaymentMethod(request.getPaymentMethod()));
        order.setPaidAt(LocalDateTime.now());
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelCurrentUserOrder(Long userId, Long orderId) {
        getRequiredUserOwnedOrder(userId, orderId);

        FoodOrderUpdateStatusRequest request = new FoodOrderUpdateStatusRequest();
        request.setOrderId(orderId);
        request.setCloseReason(CLOSE_REASON_USER_CANCEL);
        cancelOrder(request);
    }

    @Override
    public int closeExpiredUnpaidOrders(int timeoutMinutes, int batchSize) {
        int safeTimeoutMinutes = timeoutMinutes <= 0 ? 15 : timeoutMinutes;
        int safeBatchSize = batchSize <= 0 ? 50 : batchSize;
        LocalDateTime expireBefore = LocalDateTime.now().minusMinutes(safeTimeoutMinutes);

        List<FoodOrder> expiredOrders = this.list(
                new LambdaQueryWrapper<FoodOrder>()
                        .eq(FoodOrder::getOrderStatus, FoodOrderStatus.PENDING.getCode())
                        .eq(FoodOrder::getPaymentStatus, FoodOrderPaymentStatus.UNPAID.getCode())
                        .le(FoodOrder::getCreatedAt, expireBefore)
                        .orderByAsc(FoodOrder::getId)
                        .last("limit " + safeBatchSize)
        );

        int closedCount = 0;
        for (FoodOrder order : expiredOrders) {
            FoodOrderUpdateStatusRequest request = new FoodOrderUpdateStatusRequest();
            request.setOrderId(order.getId());
            request.setCloseReason(CLOSE_REASON_AUTO_CLOSE);
            try {
                self.cancelOrder(request);
                closedCount++;
            } catch (BusinessException e) {
                log.warn("Skip auto-closing order {}: {}", order.getId(), e.getMessage());
            }
        }
        return closedCount;
    }

    @Override
    public OrderStatOverviewResponse getOrderStatOverview() {
        return getOrderStatOverview(null);
    }

    @Override
    public OrderStatOverviewResponse getOrderStatOverview(OrderStatQuery query) {
        List<FoodOrder> orderList = this.list(buildStatOrderQueryWrapper(query));

        long pendingCount = 0L;
        long processingCount = 0L;
        long completedCount = 0L;
        long canceledCount = 0L;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal completedAmount = BigDecimal.ZERO;

        for (FoodOrder order : orderList) {
            if (order.getTotalAmount() != null) {
                totalAmount = totalAmount.add(order.getTotalAmount());
            }

            Integer orderStatus = order.getOrderStatus();

            if (Integer.valueOf(FoodOrderStatus.PENDING.getCode()).equals(orderStatus)) {
                pendingCount++;
            } else if (Integer.valueOf(FoodOrderStatus.PROCESSING.getCode()).equals(orderStatus)) {
                processingCount++;
            } else if (Integer.valueOf(FoodOrderStatus.COMPLETED.getCode()).equals(orderStatus)) {
                completedCount++;
                if (order.getTotalAmount() != null) {
                    completedAmount = completedAmount.add(order.getTotalAmount());
                }
            } else if (Integer.valueOf(FoodOrderStatus.CANCELED.getCode()).equals(orderStatus)) {
                canceledCount++;
            }
        }

        OrderStatOverviewResponse response = new OrderStatOverviewResponse();
        response.setTotalOrderCount((long) orderList.size());
        response.setPendingOrderCount(pendingCount);
        response.setProcessingOrderCount(processingCount);
        response.setCompletedOrderCount(completedCount);
        response.setCanceledOrderCount(canceledCount);
        response.setTotalAmount(totalAmount);
        response.setCompletedAmount(completedAmount);
        return response;
    }

    @Override
    public List<OrderStatusCountResponse> getOrderStatusCounts() {
        return getOrderStatusCounts(null);
    }

    @Override
    public List<OrderStatusCountResponse> getOrderStatusCounts(OrderStatQuery query) {
        List<FoodOrder> orderList = this.list(buildStatOrderQueryWrapper(query));

        Map<Integer, Long> countMap = new LinkedHashMap<>();
        for (FoodOrderStatus status : FoodOrderStatus.values()) {
            countMap.put(status.getCode(), 0L);
        }

        for (FoodOrder order : orderList) {
            Integer status = order.getOrderStatus();
            if (countMap.containsKey(status)) {
                countMap.put(status, countMap.get(status) + 1);
            }
        }

        List<OrderStatusCountResponse> responseList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : countMap.entrySet()) {
            OrderStatusCountResponse response = new OrderStatusCountResponse();
            response.setOrderStatus(entry.getKey());
            response.setOrderStatusLabel(FoodOrderStatus.getLabelByCode(entry.getKey()));
            response.setOrderCount(entry.getValue());
            responseList.add(response);
        }

        return responseList;
    }

    @Override
    public List<OrderPaymentStatusCountResponse> getOrderPaymentStatusCounts() {
        return getOrderPaymentStatusCounts(null);
    }

    @Override
    public List<OrderPaymentStatusCountResponse> getOrderPaymentStatusCounts(OrderStatQuery query) {
        List<FoodOrder> orderList = this.list(buildStatOrderQueryWrapper(query));

        Map<Integer, Long> countMap = new LinkedHashMap<>();
        for (FoodOrderPaymentStatus status : FoodOrderPaymentStatus.values()) {
            countMap.put(status.getCode(), 0L);
        }

        for (FoodOrder order : orderList) {
            Integer paymentStatus = normalizePaymentStatus(order.getPaymentStatus());
            if (countMap.containsKey(paymentStatus)) {
                countMap.put(paymentStatus, countMap.get(paymentStatus) + 1);
            }
        }

        List<OrderPaymentStatusCountResponse> responseList = new ArrayList<>();
        for (Map.Entry<Integer, Long> entry : countMap.entrySet()) {
            OrderPaymentStatusCountResponse response = new OrderPaymentStatusCountResponse();
            response.setPaymentStatus(entry.getKey());
            response.setPaymentStatusLabel(FoodOrderPaymentStatus.getLabelByCode(entry.getKey()));
            response.setOrderCount(entry.getValue());
            responseList.add(response);
        }
        return responseList;
    }

    @Override
    public List<OrderTopItemResponse> getTopSellingItems() {
        return getTopSellingItems(null);
    }

    @Override
    public List<OrderTopItemResponse> getTopSellingItems(OrderStatQuery query) {
        List<FoodOrder> completedOrders = this.list(
                buildStatOrderQueryWrapper(query)
                        .eq(FoodOrder::getOrderStatus, FoodOrderStatus.COMPLETED.getCode())
        );
        if (completedOrders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> orderIds = completedOrders.stream()
                .map(FoodOrder::getId)
                .toList();

        List<FoodOrderItem> orderItemList = foodOrderItemMapper.selectList(
                new LambdaQueryWrapper<FoodOrderItem>()
                        .in(FoodOrderItem::getOrderId, orderIds)
        );

        Map<Long, OrderTopItemResponse> itemStatMap = new LinkedHashMap<>();
        for (FoodOrderItem orderItem : orderItemList) {
            OrderTopItemResponse stat = itemStatMap.get(orderItem.getFoodItemId());
            if (stat == null) {
                stat = new OrderTopItemResponse();
                stat.setFoodItemId(orderItem.getFoodItemId());
                stat.setFoodName(orderItem.getFoodNameSnapshot());
                stat.setTotalQuantity(0L);
                stat.setTotalAmount(BigDecimal.ZERO);
                itemStatMap.put(orderItem.getFoodItemId(), stat);
            }

            stat.setTotalQuantity(stat.getTotalQuantity() + orderItem.getQuantity());
            stat.setTotalAmount(stat.getTotalAmount().add(orderItem.getAmount()));
        }

        List<OrderTopItemResponse> responseList = new ArrayList<>(itemStatMap.values());
        responseList.sort((a, b) -> Long.compare(b.getTotalQuantity(), a.getTotalQuantity()));
        return responseList;
    }

    @Override
    public List<OrderDailyAmountResponse> getDailyAmounts() {
        return getDailyAmounts(null);
    }

    @Override
    public List<OrderDailyAmountResponse> getDailyAmounts(OrderStatQuery query) {
        List<FoodOrder> completedOrders = this.list(
                buildStatOrderQueryWrapper(query)
                        .eq(FoodOrder::getOrderStatus, FoodOrderStatus.COMPLETED.getCode())
                        .orderByAsc(FoodOrder::getCreatedAt)
        );

        Map<String, BigDecimal> amountMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (FoodOrder order : completedOrders) {
            String statDate = order.getCreatedAt().format(formatter);
            BigDecimal oldAmount = amountMap.getOrDefault(statDate, BigDecimal.ZERO);
            amountMap.put(statDate, oldAmount.add(order.getTotalAmount()));
        }

        List<OrderDailyAmountResponse> responseList = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amountMap.entrySet()) {
            OrderDailyAmountResponse response = new OrderDailyAmountResponse();
            response.setStatDate(entry.getKey());
            response.setTotalAmount(entry.getValue());
            responseList.add(response);
        }

        return responseList;
    }
}
