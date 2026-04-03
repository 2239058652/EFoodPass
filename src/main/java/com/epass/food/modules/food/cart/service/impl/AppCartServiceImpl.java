package com.epass.food.modules.food.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.food.cart.dto.AppCartAddItemRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutResponse;
import com.epass.food.modules.food.cart.dto.AppCartItemResponse;
import com.epass.food.modules.food.cart.dto.AppCartResponse;
import com.epass.food.modules.food.cart.dto.AppCartUpdateItemRequest;
import com.epass.food.modules.food.cart.entity.FoodCartItem;
import com.epass.food.modules.food.cart.mapper.FoodCartItemMapper;
import com.epass.food.modules.food.cart.service.AppCartService;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import com.epass.food.modules.food.order.dto.AppOrderCreateRequest;
import com.epass.food.modules.food.order.dto.FoodOrderItemRequest;
import com.epass.food.modules.food.order.service.FoodOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppCartServiceImpl implements AppCartService {

    private final FoodCartItemMapper foodCartItemMapper;
    private final FoodItemMapper foodItemMapper;
    private final FoodCategoryMapper foodCategoryMapper;
    private final FoodOrderService foodOrderService;

    public AppCartServiceImpl(FoodCartItemMapper foodCartItemMapper,
                              FoodItemMapper foodItemMapper,
                              FoodCategoryMapper foodCategoryMapper,
                              FoodOrderService foodOrderService) {
        this.foodCartItemMapper = foodCartItemMapper;
        this.foodItemMapper = foodItemMapper;
        this.foodCategoryMapper = foodCategoryMapper;
        this.foodOrderService = foodOrderService;
    }

    @Override
    public AppCartResponse getCurrentUserCart(Long userId) {
        return buildCartResponse(listCartItems(userId));
    }

    @Override
    public void addItem(Long userId, AppCartAddItemRequest request) {
        FoodItem item = getRequiredAvailableItem(request.getFoodItemId());
        FoodCartItem existingItem = findCartItem(userId, request.getFoodItemId());

        int targetQuantity = request.getQuantity();
        if (existingItem != null && existingItem.getQuantity() != null) {
            targetQuantity += existingItem.getQuantity();
        }

        validateCartStock(item, targetQuantity);

        if (existingItem == null) {
            FoodCartItem cartItem = new FoodCartItem();
            cartItem.setUserId(userId);
            cartItem.setFoodItemId(request.getFoodItemId());
            cartItem.setQuantity(request.getQuantity());
            foodCartItemMapper.insert(cartItem);
            return;
        }

        existingItem.setQuantity(targetQuantity);
        foodCartItemMapper.updateById(existingItem);
    }

    @Override
    public void updateQuantity(Long userId, Long foodItemId, AppCartUpdateItemRequest request) {
        FoodCartItem cartItem = getRequiredCartItem(userId, foodItemId);
        FoodItem item = getRequiredAvailableItem(foodItemId);
        validateCartStock(item, request.getQuantity());

        cartItem.setQuantity(request.getQuantity());
        foodCartItemMapper.updateById(cartItem);
    }

    @Override
    public void removeItem(Long userId, Long foodItemId) {
        FoodCartItem cartItem = getRequiredCartItem(userId, foodItemId);
        foodCartItemMapper.deleteById(cartItem.getId());
    }

    @Override
    public void clearCart(Long userId) {
        foodCartItemMapper.delete(
                new LambdaQueryWrapper<FoodCartItem>()
                        .eq(FoodCartItem::getUserId, userId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AppCartCheckoutResponse checkout(Long userId, AppCartCheckoutRequest request) {
        List<FoodCartItem> cartItems = listCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new BusinessException(BizErrorCode.CART_EMPTY, "购物车为空，无法结算");
        }

        CartBuildResult buildResult = buildCheckoutRequest(cartItems, request == null ? null : request.getRemark());
        foodOrderService.createCurrentUserOrder(userId, buildResult.request());
        clearCart(userId);

        AppCartCheckoutResponse response = new AppCartCheckoutResponse();
        response.setTotalQuantity(buildResult.totalQuantity());
        response.setTotalAmount(buildResult.totalAmount());
        return response;
    }

    private AppCartResponse buildCartResponse(List<FoodCartItem> cartItems) {
        AppCartResponse response = new AppCartResponse();
        response.setItems(new ArrayList<>());
        response.setTotalQuantity(0);
        response.setTotalAmount(BigDecimal.ZERO);
        response.setInvalidItemCount(0);
        response.setCanCheckout(false);

        if (cartItems.isEmpty()) {
            return response;
        }

        CartData cartData = loadCartData(cartItems);
        List<AppCartItemResponse> itemResponses = new ArrayList<>();
        int totalQuantity = 0;
        int invalidItemCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (FoodCartItem cartItem : cartItems) {
            FoodItem item = cartData.itemMap().get(cartItem.getFoodItemId());
            FoodCategory category = item == null ? null : cartData.categoryMap().get(item.getCategoryId());

            AppCartItemResponse itemResponse = new AppCartItemResponse();
            itemResponse.setFoodItemId(cartItem.getFoodItemId());
            itemResponse.setQuantity(cartItem.getQuantity());

            if (item != null) {
                itemResponse.setCategoryId(item.getCategoryId());
                itemResponse.setName(item.getName());
                itemResponse.setPrice(item.getPrice());
                itemResponse.setStock(item.getStock());
                itemResponse.setSoldOut(item.getStock() == null || item.getStock() <= 0);
                if (item.getPrice() != null && cartItem.getQuantity() != null) {
                    itemResponse.setAmount(item.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
                }
            } else {
                itemResponse.setSoldOut(true);
            }

            if (category != null) {
                itemResponse.setCategoryName(category.getName());
            }

            ValidationResult validationResult = validateCartItem(cartItem, item, category);
            itemResponse.setAvailable(validationResult.available());
            itemResponse.setUnavailableReason(validationResult.message());

            itemResponses.add(itemResponse);
            totalQuantity += cartItem.getQuantity() == null ? 0 : cartItem.getQuantity();

            if (validationResult.available()) {
                totalAmount = totalAmount.add(itemResponse.getAmount());
            } else {
                invalidItemCount++;
            }
        }

        response.setItems(itemResponses);
        response.setTotalQuantity(totalQuantity);
        response.setTotalAmount(totalAmount);
        response.setInvalidItemCount(invalidItemCount);
        response.setCanCheckout(invalidItemCount == 0 && !itemResponses.isEmpty());
        return response;
    }

    private CartBuildResult buildCheckoutRequest(List<FoodCartItem> cartItems, String remark) {
        CartData cartData = loadCartData(cartItems);
        List<FoodOrderItemRequest> orderItems = new ArrayList<>();
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (FoodCartItem cartItem : cartItems) {
            FoodItem item = cartData.itemMap().get(cartItem.getFoodItemId());
            FoodCategory category = item == null ? null : cartData.categoryMap().get(item.getCategoryId());
            ValidationResult validationResult = validateCartItem(cartItem, item, category);
            if (!validationResult.available()) {
                throw new BusinessException(BizErrorCode.CART_HAS_INVALID_ITEMS, validationResult.message());
            }

            FoodOrderItemRequest orderItem = new FoodOrderItemRequest();
            orderItem.setFoodItemId(cartItem.getFoodItemId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItems.add(orderItem);

            totalQuantity += cartItem.getQuantity();
            totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        AppOrderCreateRequest orderRequest = new AppOrderCreateRequest();
        orderRequest.setRemark(StringUtils.hasText(remark) ? remark.trim() : null);
        orderRequest.setItems(orderItems);
        return new CartBuildResult(orderRequest, totalQuantity, totalAmount);
    }

    private List<FoodCartItem> listCartItems(Long userId) {
        return foodCartItemMapper.selectList(
                new LambdaQueryWrapper<FoodCartItem>()
                        .eq(FoodCartItem::getUserId, userId)
                        .orderByDesc(FoodCartItem::getId)
        );
    }

    private FoodCartItem getRequiredCartItem(Long userId, Long foodItemId) {
        FoodCartItem cartItem = findCartItem(userId, foodItemId);
        if (cartItem == null) {
            throw new BusinessException(BizErrorCode.CART_ITEM_NOT_FOUND, "购物车中不存在该菜品");
        }
        return cartItem;
    }

    private FoodCartItem findCartItem(Long userId, Long foodItemId) {
        List<FoodCartItem> cartItems = foodCartItemMapper.selectList(
                new LambdaQueryWrapper<FoodCartItem>()
                        .eq(FoodCartItem::getUserId, userId)
                        .eq(FoodCartItem::getFoodItemId, foodItemId)
                        .last("limit 1")
        );
        return cartItems.isEmpty() ? null : cartItems.get(0);
    }

    private FoodItem getRequiredAvailableItem(Long foodItemId) {
        FoodItem item = foodItemMapper.selectById(foodItemId);
        if (item == null || !Integer.valueOf(1).equals(item.getIsOnSale())) {
            throw new BusinessException(BizErrorCode.CART_ITEM_UNAVAILABLE, "菜品不存在或已下架");
        }

        FoodCategory category = foodCategoryMapper.selectById(item.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException(BizErrorCode.CART_ITEM_UNAVAILABLE, "菜品不存在或已下架");
        }
        return item;
    }

    private void validateCartStock(FoodItem item, Integer quantity) {
        if (item.getStock() == null || item.getStock() < quantity) {
            throw new BusinessException(BizErrorCode.CART_ITEM_STOCK_NOT_ENOUGH, "菜品库存不足");
        }
    }

    private CartData loadCartData(List<FoodCartItem> cartItems) {
        List<Long> itemIds = cartItems.stream()
                .map(FoodCartItem::getFoodItemId)
                .distinct()
                .toList();

        Map<Long, FoodItem> itemMap = new LinkedHashMap<>();
        if (!itemIds.isEmpty()) {
            List<FoodItem> itemList = foodItemMapper.selectBatchIds(itemIds);
            for (FoodItem item : itemList) {
                itemMap.put(item.getId(), item);
            }
        }

        List<Long> categoryIds = itemMap.values().stream()
                .map(FoodItem::getCategoryId)
                .distinct()
                .toList();

        Map<Long, FoodCategory> categoryMap = new LinkedHashMap<>();
        if (!categoryIds.isEmpty()) {
            List<FoodCategory> categories = foodCategoryMapper.selectBatchIds(categoryIds);
            for (FoodCategory category : categories) {
                categoryMap.put(category.getId(), category);
            }
        }

        return new CartData(itemMap, categoryMap);
    }

    private ValidationResult validateCartItem(FoodCartItem cartItem, FoodItem item, FoodCategory category) {
        if (item == null) {
            return new ValidationResult(false, "购物车中存在已失效的菜品，请先移除");
        }
        if (!Integer.valueOf(1).equals(item.getIsOnSale())) {
            return new ValidationResult(false, "购物车中存在已下架的菜品，请先调整");
        }
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            return new ValidationResult(false, "购物车中存在不可用分类下的菜品，请先调整");
        }
        if (item.getStock() == null || item.getStock() < cartItem.getQuantity()) {
            return new ValidationResult(false, "购物车中存在库存不足的菜品，请先调整");
        }
        return new ValidationResult(true, null);
    }

    private record CartData(Map<Long, FoodItem> itemMap, Map<Long, FoodCategory> categoryMap) {
    }

    private record ValidationResult(boolean available, String message) {
    }

    private record CartBuildResult(AppOrderCreateRequest request, Integer totalQuantity, BigDecimal totalAmount) {
    }
}
