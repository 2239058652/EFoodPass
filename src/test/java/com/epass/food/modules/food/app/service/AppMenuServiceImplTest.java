package com.epass.food.modules.food.app.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.app.dto.AppMenuCategoryResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemDetailResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemListQuery;
import com.epass.food.modules.food.app.dto.AppMenuItemResponse;
import com.epass.food.modules.food.app.service.impl.AppMenuServiceImpl;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppMenuServiceImplTest {

    @Mock
    private FoodCategoryMapper foodCategoryMapper;

    @Mock
    private FoodItemMapper foodItemMapper;

    private AppMenuService appMenuService;

    @BeforeEach
    void setUp() {
        appMenuService = new AppMenuServiceImpl(foodCategoryMapper, foodItemMapper);
    }

    @Test
    void listMenuTreeShouldGroupItemsUnderEnabledCategories() {
        when(foodCategoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "Main", 1, 1),
                category(2L, "Drink", 2, 1)
        ));
        when(foodItemMapper.selectList(any())).thenReturn(List.of(
                item(11L, 1L, "Rice", "12.00", 6, 1),
                item(12L, 1L, "Soup", "10.00", 0, 1)
        ));

        List<AppMenuCategoryResponse> result = appMenuService.listMenuTree();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Main");
        assertThat(result.get(0).getItems()).hasSize(2);
        assertThat(result.get(0).getItems().get(1).isSoldOut()).isTrue();
    }

    @Test
    void listAvailableItemsShouldReturnEmptyPageForDisabledCategoryFilter() {
        when(foodCategoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "Main", 1, 1)
        ));

        AppMenuItemListQuery query = new AppMenuItemListQuery();
        query.setCategoryId(99L);

        PageResult<AppMenuItemResponse> result = appMenuService.listAvailableItems(query);

        assertThat(result.getTotal()).isZero();
        assertThat(result.getRecords()).isEmpty();
        verify(foodItemMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    void listAvailableItemsShouldMapPageRecords() {
        when(foodCategoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "Main", 1, 1)
        ));
        when(foodItemMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<FoodItem> page = invocation.getArgument(0);
            page.setRecords(List.of(item(15L, 1L, "Noodles", "16.00", 3, 1)));
            page.setTotal(1L);
            return page;
        });

        AppMenuItemListQuery query = new AppMenuItemListQuery();
        query.setPageNum(2L);
        query.setPageSize(5L);

        PageResult<AppMenuItemResponse> result = appMenuService.listAvailableItems(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getPageNum()).isEqualTo(2L);
        assertThat(result.getRecords()).hasSize(1);
        assertThat(result.getRecords().get(0).getCategoryName()).isEqualTo("Main");
    }

    @Test
    void getAvailableItemDetailShouldRejectOffSaleItem() {
        when(foodItemMapper.selectById(20L)).thenReturn(item(20L, 1L, "Hidden", "9.00", 2, 0));

        assertThatThrownBy(() -> appMenuService.getAvailableItemDetail(20L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("菜品不存在或已下架");
    }

    @Test
    void getAvailableItemDetailShouldReturnDetailForAvailableItem() {
        when(foodItemMapper.selectById(21L)).thenReturn(item(21L, 1L, "Tea", "5.00", 0, 1));
        when(foodCategoryMapper.selectById(1L)).thenReturn(category(1L, "Drink", 1, 1));

        AppMenuItemDetailResponse result = appMenuService.getAvailableItemDetail(21L);

        assertThat(result.getName()).isEqualTo("Tea");
        assertThat(result.getCategoryName()).isEqualTo("Drink");
        assertThat(result.isSoldOut()).isTrue();
    }

    private FoodCategory category(Long id, String name, Integer sortNo, Integer status) {
        FoodCategory category = new FoodCategory();
        category.setId(id);
        category.setName(name);
        category.setSortNo(sortNo);
        category.setStatus(status);
        return category;
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
        item.setDescription(name + " description");
        return item;
    }
}
