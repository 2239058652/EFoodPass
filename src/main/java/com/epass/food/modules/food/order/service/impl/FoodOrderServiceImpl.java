package com.epass.food.modules.food.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import com.epass.food.modules.food.order.dto.*;
import com.epass.food.modules.food.order.entity.FoodOrder;
import com.epass.food.modules.food.order.entity.FoodOrderItem;
import com.epass.food.modules.food.order.mapper.FoodOrderItemMapper;
import com.epass.food.modules.food.order.mapper.FoodOrderMapper;
import com.epass.food.modules.food.order.service.FoodOrderService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class FoodOrderServiceImpl extends ServiceImpl<FoodOrderMapper, FoodOrder> implements FoodOrderService {

    private static final int ORDER_STATUS_PENDING = 10;
    private static final int ORDER_STATUS_PROCESSING = 20;
    private static final int ORDER_STATUS_COMPLETED = 30;
    private static final int ORDER_STATUS_CANCELED = 40;

    private final FoodOrderItemMapper foodOrderItemMapper;
    private final FoodItemMapper foodItemMapper;
    private final FoodCategoryMapper foodCategoryMapper;
    private final SysUserMapper sysUserMapper;

    public FoodOrderServiceImpl(FoodOrderItemMapper foodOrderItemMapper,
                                FoodItemMapper foodItemMapper,
                                FoodCategoryMapper foodCategoryMapper,
                                SysUserMapper sysUserMapper) {
        this.foodOrderItemMapper = foodOrderItemMapper;
        this.foodItemMapper = foodItemMapper;
        this.foodCategoryMapper = foodCategoryMapper;
        this.sysUserMapper = sysUserMapper;
    }

    private void validateOrderStatus(Integer orderStatus) {
        if (!Integer.valueOf(ORDER_STATUS_PENDING).equals(orderStatus)
                && !Integer.valueOf(ORDER_STATUS_PROCESSING).equals(orderStatus)
                && !Integer.valueOf(ORDER_STATUS_COMPLETED).equals(orderStatus)
                && !Integer.valueOf(ORDER_STATUS_CANCELED).equals(orderStatus)) {
            throw new BusinessException(4301, "订单状态值不合法");
        }
    }

    private FoodOrder getRequiredOrder(Long orderId) {
        FoodOrder order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException(4302, "订单不存在");
        }
        return order;
    }

    private SysUser getRequiredUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(4303, "下单用户不存在");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(4312, "下单用户已被禁用");
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
        response.setRemark(order.getRemark());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    @Override
    public PageResult<FoodOrderListResponse> listOrders(FoodOrderListQuery query) {
        if (query == null) {
            query = new FoodOrderListQuery();
        }

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

        queryWrapper.orderByDesc(FoodOrder::getId);

        Page<FoodOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<FoodOrder> orderPage = this.page(page, queryWrapper);

        List<FoodOrderListResponse> responseList = new ArrayList<>();
        for (FoodOrder order : orderPage.getRecords()) {
            responseList.add(buildListResponse(order));
        }

        PageResult<FoodOrderListResponse> result = new PageResult<>();
        result.setTotal(orderPage.getTotal());
        result.setPageNum(orderPage.getCurrent());
        result.setPageSize(orderPage.getSize());
        result.setRecords(responseList);
        return result;
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
        response.setRemark(order.getRemark());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        response.setItems(itemResponses);
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(FoodOrderCreateRequest request) {
        getRequiredUser(request.getUserId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(4304, "订单明细不能为空");
        }

        Map<Long, Integer> itemQuantityMap = new HashMap<>();
        for (FoodOrderItemRequest itemRequest : request.getItems()) {
            itemQuantityMap.merge(itemRequest.getFoodItemId(), itemRequest.getQuantity(), Integer::sum);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<FoodOrderItem> orderItems = new ArrayList<>();
        List<FoodItem> itemsToUpdate = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : itemQuantityMap.entrySet()) {
            Long foodItemId = entry.getKey();
            Integer totalQuantity = entry.getValue();

            FoodItem item = foodItemMapper.selectById(foodItemId);
            if (item == null) {
                throw new BusinessException(4305, "菜品不存在");
            }

            if (!Integer.valueOf(1).equals(item.getIsOnSale())) {
                throw new BusinessException(4306, "存在未上架菜品，不能下单");
            }

            FoodCategory category = foodCategoryMapper.selectById(item.getCategoryId());
            if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
                throw new BusinessException(4307, "存在所属分类不可用的菜品，不能下单");
            }

            if (item.getStock() == null || item.getStock() < totalQuantity) {
                throw new BusinessException(4313, "菜品库存不足，不能下单");
            }

            item.setStock(item.getStock() - totalQuantity);
            itemsToUpdate.add(item);
        }

        for (FoodOrderItemRequest itemRequest : request.getItems()) {
            FoodItem item = foodItemMapper.selectById(itemRequest.getFoodItemId());

            BigDecimal itemAmount = item.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
            totalAmount = totalAmount.add(itemAmount);

            FoodOrderItem orderItem = new FoodOrderItem();
            orderItem.setFoodItemId(item.getId());
            orderItem.setFoodNameSnapshot(item.getName());
            orderItem.setPriceSnapshot(item.getPrice());
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setAmount(itemAmount);
            orderItems.add(orderItem);
        }

        FoodOrder order = new FoodOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setTotalAmount(totalAmount);
        order.setOrderStatus(ORDER_STATUS_PENDING);
        order.setRemark(request.getRemark());

        this.save(order);

        for (FoodOrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            foodOrderItemMapper.insert(orderItem);
        }

        for (FoodItem item : itemsToUpdate) {
            foodItemMapper.updateById(item);
        }
    }

    @Override
    public void processOrder(FoodOrderUpdateStatusRequest request) {
        FoodOrder order = getRequiredOrder(request.getOrderId());

        if (!Integer.valueOf(ORDER_STATUS_PENDING).equals(order.getOrderStatus())) {
            throw new BusinessException(4311, "只有待确认订单才能开始制作");
        }

        order.setOrderStatus(ORDER_STATUS_PROCESSING);
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(FoodOrderUpdateStatusRequest request) {
        FoodOrder order = getRequiredOrder(request.getOrderId());

        if (Integer.valueOf(ORDER_STATUS_COMPLETED).equals(order.getOrderStatus())) {
            throw new BusinessException(4308, "已完成订单不能取消");
        }

        if (Integer.valueOf(ORDER_STATUS_CANCELED).equals(order.getOrderStatus())) {
            throw new BusinessException(4309, "订单已取消，请勿重复操作");
        }

        List<FoodOrderItem> orderItemList = foodOrderItemMapper.selectList(
                new LambdaQueryWrapper<FoodOrderItem>()
                        .eq(FoodOrderItem::getOrderId, request.getOrderId())
        );

        for (FoodOrderItem orderItem : orderItemList) {
            FoodItem item = foodItemMapper.selectById(orderItem.getFoodItemId());
            if (item == null) {
                throw new BusinessException(4314, "订单关联菜品不存在，无法回补库存");
            }

            int oldStock = item.getStock() == null ? 0 : item.getStock();
            item.setStock(oldStock + orderItem.getQuantity());
            foodItemMapper.updateById(item);
        }

        order.setOrderStatus(ORDER_STATUS_CANCELED);
        this.updateById(order);
    }

    @Override
    public void completeOrder(FoodOrderUpdateStatusRequest request) {
        FoodOrder order = getRequiredOrder(request.getOrderId());

        if (!Integer.valueOf(ORDER_STATUS_PROCESSING).equals(order.getOrderStatus())) {
            throw new BusinessException(4310, "只有制作中的订单才能完成");
        }

        order.setOrderStatus(ORDER_STATUS_COMPLETED);
        this.updateById(order);
    }
}