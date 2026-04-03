package com.epass.food.modules.food.cart.service;

import com.epass.food.modules.food.cart.dto.AppCartAddItemRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutResponse;
import com.epass.food.modules.food.cart.dto.AppCartResponse;
import com.epass.food.modules.food.cart.dto.AppCartUpdateItemRequest;

public interface AppCartService {

    AppCartResponse getCurrentUserCart(Long userId);

    void addItem(Long userId, AppCartAddItemRequest request);

    void updateQuantity(Long userId, Long foodItemId, AppCartUpdateItemRequest request);

    void removeItem(Long userId, Long foodItemId);

    void clearCart(Long userId);

    AppCartCheckoutResponse checkout(Long userId, AppCartCheckoutRequest request);
}
