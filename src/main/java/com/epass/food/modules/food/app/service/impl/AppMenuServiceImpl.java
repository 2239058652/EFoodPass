package com.epass.food.modules.food.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.food.app.dto.AppMenuCategoryResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemDetailResponse;
import com.epass.food.modules.food.app.dto.AppMenuItemListQuery;
import com.epass.food.modules.food.app.dto.AppMenuItemResponse;
import com.epass.food.modules.food.app.service.AppMenuService;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AppMenuServiceImpl implements AppMenuService {

    private final FoodCategoryMapper foodCategoryMapper;
    private final FoodItemMapper foodItemMapper;

    public AppMenuServiceImpl(FoodCategoryMapper foodCategoryMapper,
                              FoodItemMapper foodItemMapper) {
        this.foodCategoryMapper = foodCategoryMapper;
        this.foodItemMapper = foodItemMapper;
    }

    @Override
    public List<AppMenuCategoryResponse> listMenuTree() {
        List<FoodCategory> categories = listEnabledCategories();
        if (categories.isEmpty()) {
            return List.of();
        }

        Map<Long, FoodCategory> categoryMap = new LinkedHashMap<>();
        List<Long> categoryIds = new ArrayList<>();
        for (FoodCategory category : categories) {
            categoryMap.put(category.getId(), category);
            categoryIds.add(category.getId());
        }

        List<FoodItem> items = foodItemMapper.selectList(
                new LambdaQueryWrapper<FoodItem>()
                        .in(FoodItem::getCategoryId, categoryIds)
                        .eq(FoodItem::getIsOnSale, 1)
                        .orderByAsc(FoodItem::getCategoryId)
                        .orderByDesc(FoodItem::getId)
        );

        Map<Long, List<AppMenuItemResponse>> itemsByCategory = new LinkedHashMap<>();
        for (FoodItem item : items) {
            FoodCategory category = categoryMap.get(item.getCategoryId());
            if (category == null) {
                continue;
            }
            itemsByCategory.computeIfAbsent(item.getCategoryId(), key -> new ArrayList<>())
                    .add(buildItemResponse(item, category.getName()));
        }

        List<AppMenuCategoryResponse> responseList = new ArrayList<>();
        for (FoodCategory category : categories) {
            List<AppMenuItemResponse> categoryItems = itemsByCategory.get(category.getId());
            if (categoryItems == null || categoryItems.isEmpty()) {
                continue;
            }

            AppMenuCategoryResponse response = new AppMenuCategoryResponse();
            response.setId(category.getId());
            response.setName(category.getName());
            response.setSortNo(category.getSortNo());
            response.setItems(categoryItems);
            responseList.add(response);
        }
        return responseList;
    }

    @Override
    public PageResult<AppMenuItemResponse> listAvailableItems(AppMenuItemListQuery query) {
        if (query == null) {
            query = new AppMenuItemListQuery();
        }

        List<FoodCategory> categories = listEnabledCategories();
        Map<Long, String> categoryNameMap = new LinkedHashMap<>();
        List<Long> categoryIds = new ArrayList<>();
        for (FoodCategory category : categories) {
            categoryNameMap.put(category.getId(), category.getName());
            categoryIds.add(category.getId());
        }

        if (categoryIds.isEmpty()) {
            return emptyPage(query);
        }

        if (query.getCategoryId() != null && !categoryNameMap.containsKey(query.getCategoryId())) {
            return emptyPage(query);
        }

        LambdaQueryWrapper<FoodItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FoodItem::getIsOnSale, 1);

        if (StringUtils.hasText(query.getName())) {
            queryWrapper.like(FoodItem::getName, query.getName().trim());
        }

        if (query.getCategoryId() != null) {
            queryWrapper.eq(FoodItem::getCategoryId, query.getCategoryId());
        } else {
            queryWrapper.in(FoodItem::getCategoryId, categoryIds);
        }

        queryWrapper.orderByDesc(FoodItem::getId);

        Page<FoodItem> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<FoodItem> itemPage = foodItemMapper.selectPage(page, queryWrapper);

        List<AppMenuItemResponse> records = new ArrayList<>();
        for (FoodItem item : itemPage.getRecords()) {
            String categoryName = categoryNameMap.get(item.getCategoryId());
            if (categoryName == null) {
                continue;
            }
            records.add(buildItemResponse(item, categoryName));
        }

        PageResult<AppMenuItemResponse> result = new PageResult<>();
        result.setTotal(itemPage.getTotal());
        result.setPageNum(itemPage.getCurrent());
        result.setPageSize(itemPage.getSize());
        result.setRecords(records);
        return result;
    }

    @Override
    public AppMenuItemDetailResponse getAvailableItemDetail(Long itemId) {
        FoodItem item = foodItemMapper.selectById(itemId);
        if (item == null || !Integer.valueOf(1).equals(item.getIsOnSale())) {
            throw new BusinessException(BizErrorCode.ITEM_NOT_FOUND, "菜品不存在或已下架");
        }

        FoodCategory category = foodCategoryMapper.selectById(item.getCategoryId());
        if (category == null || !Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException(BizErrorCode.ITEM_NOT_FOUND, "菜品不存在或已下架");
        }

        AppMenuItemDetailResponse response = new AppMenuItemDetailResponse();
        response.setId(item.getId());
        response.setCategoryId(item.getCategoryId());
        response.setCategoryName(category.getName());
        response.setName(item.getName());
        response.setPrice(item.getPrice());
        response.setStock(item.getStock());
        response.setSoldOut(isSoldOut(item));
        response.setDescription(item.getDescription());
        return response;
    }

    private List<FoodCategory> listEnabledCategories() {
        return foodCategoryMapper.selectList(
                new LambdaQueryWrapper<FoodCategory>()
                        .eq(FoodCategory::getStatus, 1)
                        .orderByAsc(FoodCategory::getSortNo)
                        .orderByDesc(FoodCategory::getId)
        );
    }

    private AppMenuItemResponse buildItemResponse(FoodItem item, String categoryName) {
        AppMenuItemResponse response = new AppMenuItemResponse();
        response.setId(item.getId());
        response.setCategoryId(item.getCategoryId());
        response.setCategoryName(categoryName);
        response.setName(item.getName());
        response.setPrice(item.getPrice());
        response.setStock(item.getStock());
        response.setSoldOut(isSoldOut(item));
        response.setDescription(item.getDescription());
        return response;
    }

    private boolean isSoldOut(FoodItem item) {
        return item.getStock() == null || item.getStock() <= 0;
    }

    private PageResult<AppMenuItemResponse> emptyPage(AppMenuItemListQuery query) {
        PageResult<AppMenuItemResponse> result = new PageResult<>();
        result.setTotal(0L);
        result.setPageNum(query.getPageNum());
        result.setPageSize(query.getPageSize());
        result.setRecords(List.of());
        return result;
    }
}
