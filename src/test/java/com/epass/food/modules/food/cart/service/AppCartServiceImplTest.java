package com.epass.food.modules.food.cart.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.food.cart.dto.AppCartAddItemRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutRequest;
import com.epass.food.modules.food.cart.dto.AppCartCheckoutResponse;
import com.epass.food.modules.food.cart.dto.AppCartResponse;
import com.epass.food.modules.food.cart.dto.AppCartUpdateItemRequest;
import com.epass.food.modules.food.cart.entity.FoodCartItem;
import com.epass.food.modules.food.cart.mapper.FoodCartItemMapper;
import com.epass.food.modules.food.cart.service.impl.AppCartServiceImpl;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import com.epass.food.modules.food.order.dto.AppOrderCreateRequest;
import com.epass.food.modules.food.order.service.FoodOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppCartServiceImplTest {

    @Mock
    private FoodCartItemMapper foodCartItemMapper;

    @Mock
    private FoodItemMapper foodItemMapper;

    @Mock
    private FoodCategoryMapper foodCategoryMapper;

    @Mock
    private FoodOrderService foodOrderService;

    private AppCartService appCartService;

    @BeforeEach
    void setUp() {
        appCartService = new AppCartServiceImpl(
                foodCartItemMapper,
                foodItemMapper,
                foodCategoryMapper,
                foodOrderService
        );
    }

    @Test
    void getCurrentUserCartShouldMarkInvalidItemsAndAmount() {
        when(foodCartItemMapper.selectList(any())).thenReturn(List.of(
                cartItem(1L, 7L, 11L, 2),
                cartItem(2L, 7L, 12L, 3)
        ));
        when(foodItemMapper.selectBatchIds(any())).thenReturn(List.of(
                item(11L, 1L, "Rice", "12.00", 5, 1),
                item(12L, 2L, "Tea", "6.00", 1, 1)
        ));
        when(foodCategoryMapper.selectBatchIds(any())).thenReturn(List.of(
                category(1L, "Main", 1),
                category(2L, "Drink", 1)
        ));

        AppCartResponse response = appCartService.getCurrentUserCart(7L);

        assertThat(response.getTotalQuantity()).isEqualTo(5);
        assertThat(response.getInvalidItemCount()).isEqualTo(1);
        assertThat(response.isCanCheckout()).isFalse();
        assertThat(response.getTotalAmount()).isEqualByComparingTo("24.00");
        assertThat(response.getItems().get(1).getUnavailableReason()).contains("库存不足");
    }

    @Test
    void addItemShouldMergeQuantityIntoExistingCartRow() {
        when(foodItemMapper.selectById(11L)).thenReturn(item(11L, 1L, "Rice", "12.00", 8, 1));
        when(foodCategoryMapper.selectById(1L)).thenReturn(category(1L, "Main", 1));
        when(foodCartItemMapper.selectList(any())).thenReturn(List.of(
                cartItem(3L, 7L, 11L, 2)
        ));

        AppCartAddItemRequest request = new AppCartAddItemRequest();
        request.setFoodItemId(11L);
        request.setQuantity(3);
        appCartService.addItem(7L, request);

        ArgumentCaptor<FoodCartItem> captor = ArgumentCaptor.forClass(FoodCartItem.class);
        verify(foodCartItemMapper).updateById(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
    }

    @Test
    void updateQuantityShouldRejectUnavailableItem() {
        when(foodCartItemMapper.selectList(any())).thenReturn(List.of(
                cartItem(4L, 7L, 15L, 1)
        ));
        when(foodItemMapper.selectById(15L)).thenReturn(item(15L, 1L, "Hidden", "9.00", 10, 0));

        AppCartUpdateItemRequest request = new AppCartUpdateItemRequest();
        request.setQuantity(2);

        assertThatThrownBy(() -> appCartService.updateQuantity(7L, 15L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("菜品不存在或已下架");
    }

    @Test
    void checkoutShouldCreateOrderAndClearCart() {
        when(foodCartItemMapper.selectList(any())).thenReturn(List.of(
                cartItem(1L, 9L, 21L, 2)
        ));
        when(foodItemMapper.selectBatchIds(any())).thenReturn(List.of(
                item(21L, 3L, "Noodles", "16.50", 6, 1)
        ));
        when(foodCategoryMapper.selectBatchIds(any())).thenReturn(List.of(
                category(3L, "Main", 1)
        ));

        AppCartCheckoutRequest request = new AppCartCheckoutRequest();
        request.setRemark("less spicy");
        AppCartCheckoutResponse response = appCartService.checkout(9L, request);

        verify(foodOrderService).createCurrentUserOrder(eq(9L), argThat((AppOrderCreateRequest orderRequest) ->
                orderRequest != null
                        && "less spicy".equals(orderRequest.getRemark())
                        && orderRequest.getItems() != null
                        && orderRequest.getItems().size() == 1
                        && Long.valueOf(21L).equals(orderRequest.getItems().get(0).getFoodItemId())
                        && Integer.valueOf(2).equals(orderRequest.getItems().get(0).getQuantity())
        ));
        verify(foodCartItemMapper).delete(any());
        assertThat(response.getTotalQuantity()).isEqualTo(2);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("33.00");
    }

    @Test
    void checkoutShouldFailForEmptyCart() {
        when(foodCartItemMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> appCartService.checkout(10L, new AppCartCheckoutRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("购物车为空，无法结算");

        verify(foodOrderService, never()).createCurrentUserOrder(any(), any());
    }

    private FoodCartItem cartItem(Long id, Long userId, Long foodItemId, Integer quantity) {
        FoodCartItem cartItem = new FoodCartItem();
        cartItem.setId(id);
        cartItem.setUserId(userId);
        cartItem.setFoodItemId(foodItemId);
        cartItem.setQuantity(quantity);
        return cartItem;
    }

    private FoodItem item(Long id,
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

    private FoodCategory category(Long id, String name, Integer status) {
        FoodCategory category = new FoodCategory();
        category.setId(id);
        category.setName(name);
        category.setStatus(status);
        return category;
    }
}
