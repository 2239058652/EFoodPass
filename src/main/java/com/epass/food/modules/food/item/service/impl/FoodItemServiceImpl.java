package com.epass.food.modules.food.item.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.item.dto.*;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import com.epass.food.modules.food.item.service.FoodItemService;
import com.epass.food.modules.food.order.entity.FoodOrderItem;
import com.epass.food.modules.food.order.mapper.FoodOrderItemMapper;
import com.epass.food.modules.food.stock.service.FoodStockLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FoodItemServiceImpl extends ServiceImpl<FoodItemMapper, FoodItem> implements FoodItemService {

    private final FoodCategoryMapper foodCategoryMapper;
    private final FoodOrderItemMapper foodOrderItemMapper;
    private final FoodStockLogService foodStockLogService;

    public FoodItemServiceImpl(FoodCategoryMapper foodCategoryMapper,
                               FoodOrderItemMapper foodOrderItemMapper,
                               FoodStockLogService foodStockLogService) {
        this.foodCategoryMapper = foodCategoryMapper;
        this.foodOrderItemMapper = foodOrderItemMapper;
        this.foodStockLogService = foodStockLogService;
    }

    private void validateOnSaleStatus(Integer isOnSale) {
        if (!Integer.valueOf(0).equals(isOnSale) && !Integer.valueOf(1).equals(isOnSale)) {
            throw new BusinessException(BizErrorCode.ITEM_ON_SALE_STATUS_INVALID, "菜品上下架状态值不合法");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(BizErrorCode.ITEM_PRICE_INVALID, "菜品价格不能小于0");
        }
    }

    private void validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new BusinessException(BizErrorCode.ITEM_STOCK_INVALID, "菜品库存不能小于0");
        }
    }

    private String normalizeItemName(String name) {
        return name == null ? null : name.trim();
    }

    private FoodCategory getRequiredCategory(Long categoryId) {
        FoodCategory category = foodCategoryMapper.selectById(categoryId);
        if (category == null) {
            throw new BusinessException(BizErrorCode.ITEM_CATEGORY_NOT_FOUND, "分类不存在");
        }
        return category;
    }

    private FoodCategory getRequiredEnabledCategory(Long categoryId) {
        FoodCategory category = getRequiredCategory(categoryId);
        if (!Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException(BizErrorCode.ITEM_CATEGORY_DISABLED, "分类已停用，不能操作菜品");
        }
        return category;
    }

    private FoodItem getRequiredItem(Long itemId) {
        FoodItem item = this.getById(itemId);
        if (item == null) {
            throw new BusinessException(BizErrorCode.ITEM_NOT_FOUND, "菜品不存在");
        }
        return item;
    }

    private void validateItemNameUnique(Long categoryId, String name, Long excludeId) {
        LambdaQueryWrapper<FoodItem> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FoodItem::getCategoryId, categoryId)
                .eq(FoodItem::getName, name);

        if (excludeId != null) {
            queryWrapper.ne(FoodItem::getId, excludeId);
        }

        queryWrapper.last("limit 1");

        FoodItem existItem = this.getOne(queryWrapper);
        if (existItem != null) {
            throw new BusinessException(BizErrorCode.ITEM_NAME_EXISTS, "同一分类下菜品名称已存在");
        }
    }

    private Map<Long, String> buildCategoryNameMap(List<FoodItem> itemList) {
        List<Long> categoryIds = itemList.stream()
                .map(FoodItem::getCategoryId)
                .distinct()
                .toList();

        Map<Long, String> categoryNameMap = new HashMap<>();
        if (categoryIds.isEmpty()) {
            return categoryNameMap;
        }

        List<FoodCategory> categoryList = foodCategoryMapper.selectBatchIds(categoryIds);
        for (FoodCategory category : categoryList) {
            categoryNameMap.put(category.getId(), category.getName());
        }
        return categoryNameMap;
    }

    private FoodItemListResponse buildListResponse(FoodItem item, Map<Long, String> categoryNameMap) {
        FoodItemListResponse response = new FoodItemListResponse();
        response.setId(item.getId());
        response.setCategoryId(item.getCategoryId());
        response.setCategoryName(categoryNameMap.get(item.getCategoryId()));
        response.setName(item.getName());
        response.setPrice(item.getPrice());
        response.setStock(item.getStock());
        response.setIsOnSale(item.getIsOnSale());
        return response;
    }

    @Override
    public PageResult<FoodItemListResponse> listItems(FoodItemListQuery query) {
        if (query == null) {
            query = new FoodItemListQuery();
        }

        LambdaQueryWrapper<FoodItem> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getName())) {
            queryWrapper.like(FoodItem::getName, query.getName().trim());
        }

        if (query.getCategoryId() != null) {
            queryWrapper.eq(FoodItem::getCategoryId, query.getCategoryId());
        }

        if (query.getIsOnSale() != null) {
            validateOnSaleStatus(query.getIsOnSale());
            queryWrapper.eq(FoodItem::getIsOnSale, query.getIsOnSale());
        }

        queryWrapper.orderByDesc(FoodItem::getId);

        Page<FoodItem> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<FoodItem> itemPage = this.page(page, queryWrapper);

        List<FoodItem> itemList = itemPage.getRecords();
        Map<Long, String> categoryNameMap = buildCategoryNameMap(itemList);

        List<FoodItemListResponse> responseList = new ArrayList<>();
        for (FoodItem item : itemList) {
            responseList.add(buildListResponse(item, categoryNameMap));
        }

        PageResult<FoodItemListResponse> result = new PageResult<>();
        result.setTotal(itemPage.getTotal());
        result.setPageNum(itemPage.getCurrent());
        result.setPageSize(itemPage.getSize());
        result.setRecords(responseList);
        return result;
    }

    @Override
    public FoodItemDetailResponse getItemDetail(Long itemId) {
        FoodItem item = getRequiredItem(itemId);
        FoodCategory category = foodCategoryMapper.selectById(item.getCategoryId());

        FoodItemDetailResponse response = new FoodItemDetailResponse();
        response.setId(item.getId());
        response.setCategoryId(item.getCategoryId());
        response.setCategoryName(category == null ? null : category.getName());
        response.setName(item.getName());
        response.setPrice(item.getPrice());
        response.setStock(item.getStock());
        response.setIsOnSale(item.getIsOnSale());
        response.setDescription(item.getDescription());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }

    @Override
    public void createItem(FoodItemCreateRequest request) {
        String itemName = normalizeItemName(request.getName());
        if (!StringUtils.hasText(itemName)) {
            throw new BusinessException(BizErrorCode.ITEM_NAME_BLANK, "菜品名称不能为空");
        }

        getRequiredEnabledCategory(request.getCategoryId());
        validatePrice(request.getPrice());
        validateStock(request.getStock());
        validateOnSaleStatus(request.getIsOnSale());
        validateItemNameUnique(request.getCategoryId(), itemName, null);

        FoodItem item = new FoodItem();
        item.setCategoryId(request.getCategoryId());
        item.setName(itemName);
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());
        item.setIsOnSale(request.getIsOnSale());
        item.setDescription(request.getDescription());

        this.save(item);
    }

    @Override
    public void updateItem(FoodItemUpdateRequest request) {
        FoodItem item = getRequiredItem(request.getId());

        String itemName = normalizeItemName(request.getName());
        if (!StringUtils.hasText(itemName)) {
            throw new BusinessException(BizErrorCode.ITEM_NAME_BLANK, "菜品名称不能为空");
        }

        getRequiredEnabledCategory(request.getCategoryId());
        validatePrice(request.getPrice());
        validateStock(request.getStock());
        validateOnSaleStatus(request.getIsOnSale());
        validateItemNameUnique(request.getCategoryId(), itemName, request.getId());

        item.setCategoryId(request.getCategoryId());
        item.setName(itemName);
        item.setPrice(request.getPrice());
        item.setStock(request.getStock());
        item.setIsOnSale(request.getIsOnSale());
        item.setDescription(request.getDescription());

        this.updateById(item);
    }

    @Override
    public void updateOnSaleStatus(FoodItemUpdateOnSaleRequest request) {
        FoodItem item = getRequiredItem(request.getItemId());
        validateOnSaleStatus(request.getIsOnSale());

        FoodCategory category = getRequiredCategory(item.getCategoryId());
        if (Integer.valueOf(1).equals(request.getIsOnSale()) && !Integer.valueOf(1).equals(category.getStatus())) {
            throw new BusinessException(BizErrorCode.ITEM_CATEGORY_DISABLED_FOR_ON_SALE, "所属分类已停用，不能上架菜品");
        }

        item.setIsOnSale(request.getIsOnSale());
        this.updateById(item);
    }

    @Override
    public void adjustStock(FoodItemAdjustStockRequest request) {
        FoodItem item = getRequiredItem(request.getItemId());
        validateStock(request.getStock());

        int beforeStock = item.getStock() == null ? 0 : item.getStock();
        int afterStock = request.getStock();

        item.setStock(afterStock);
        this.updateById(item);

        foodStockLogService.recordManualAdjust(
                item.getId(),
                beforeStock,
                afterStock,
                StringUtils.hasText(request.getRemark()) ? request.getRemark().trim() : "后台手工调整库存"
        );
    }

    @Override
    public void deleteItem(Long itemId) {
        getRequiredItem(itemId);

        Long orderItemCount = foodOrderItemMapper.selectCount(
                new LambdaQueryWrapper<FoodOrderItem>()
                        .eq(FoodOrderItem::getFoodItemId, itemId)
        );
        if (orderItemCount != null && orderItemCount > 0) {
            throw new BusinessException(BizErrorCode.ITEM_HAS_ORDER_RELATION, "当前菜品已有关联订单，不能删除");
        }

        this.removeById(itemId);
    }
}