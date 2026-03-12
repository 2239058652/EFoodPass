package com.epass.food.modules.food.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.category.dto.*;
import com.epass.food.modules.food.category.entity.FoodCategory;
import com.epass.food.modules.food.category.mapper.FoodCategoryMapper;
import com.epass.food.modules.food.category.service.FoodCategoryService;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class FoodCategoryServiceImpl extends ServiceImpl<FoodCategoryMapper, FoodCategory> implements FoodCategoryService {

    private final FoodItemMapper foodItemMapper;

    public FoodCategoryServiceImpl(FoodItemMapper foodItemMapper) {
        this.foodItemMapper = foodItemMapper;
    }

    /**
     * 校验分类状态值
     *
     * @param status 分类状态
     */
    private void validateCategoryStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(4101, "分类状态值不合法");
        }
    }

    /**
     * 规范化分类名称
     *
     * @param name 分类名称
     * @return 去除首尾空格后的名称
     */
    private String normalizeCategoryName(String name) {
        return name == null ? null : name.trim();
    }

    /**
     * 校验分类名称是否重复
     *
     * @param name      分类名称
     * @param excludeId 需要排除的分类ID，新增时传 null
     */
    private void validateCategoryNameUnique(String name, Long excludeId) {
        LambdaQueryWrapper<FoodCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FoodCategory::getName, name);

        if (excludeId != null) {
            queryWrapper.ne(FoodCategory::getId, excludeId);
        }

        queryWrapper.last("limit 1");

        FoodCategory existCategory = this.getOne(queryWrapper);
        if (existCategory != null) {
            throw new BusinessException(4102, "分类名称已存在");
        }
    }

    /**
     * 查询分类详情，不存在则抛异常
     *
     * @param categoryId 分类ID
     * @return 分类实体
     */
    private FoodCategory getRequiredCategory(Long categoryId) {
        FoodCategory category = this.getById(categoryId);
        if (category == null) {
            throw new BusinessException(4103, "分类不存在");
        }
        return category;
    }

    /**
     * 构造列表响应对象
     *
     * @param category 分类实体
     * @return 列表响应
     */
    private FoodCategoryListResponse buildListResponse(FoodCategory category) {
        FoodCategoryListResponse response = new FoodCategoryListResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSortNo(category.getSortNo());
        response.setStatus(category.getStatus());
        return response;
    }

    @Override
    public PageResult<FoodCategoryListResponse> listCategories(FoodCategoryListQuery query) {
        if (query == null) {
            query = new FoodCategoryListQuery();
        }

        LambdaQueryWrapper<FoodCategory> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getName())) {
            queryWrapper.like(FoodCategory::getName, query.getName().trim());
        }

        if (query.getStatus() != null) {
            queryWrapper.eq(FoodCategory::getStatus, query.getStatus());
        }

        queryWrapper.orderByAsc(FoodCategory::getSortNo)
                .orderByDesc(FoodCategory::getId);

        Page<FoodCategory> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<FoodCategory> categoryPage = this.page(page, queryWrapper);

        List<FoodCategoryListResponse> responseList = new ArrayList<>();
        for (FoodCategory category : categoryPage.getRecords()) {
            responseList.add(buildListResponse(category));
        }

        PageResult<FoodCategoryListResponse> result = new PageResult<>();
        result.setTotal(categoryPage.getTotal());
        result.setPageNum(categoryPage.getCurrent());
        result.setPageSize(categoryPage.getSize());
        result.setRecords(responseList);
        return result;
    }

    @Override
    public FoodCategoryDetailResponse getCategoryDetail(Long categoryId) {
        FoodCategory category = getRequiredCategory(categoryId);

        FoodCategoryDetailResponse response = new FoodCategoryDetailResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setSortNo(category.getSortNo());
        response.setStatus(category.getStatus());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }

    @Override
    public void createCategory(FoodCategoryCreateRequest request) {
        String categoryName = normalizeCategoryName(request.getName());
        validateCategoryStatus(request.getStatus());
        validateCategoryNameUnique(categoryName, null);

        FoodCategory category = new FoodCategory();
        category.setName(categoryName);
        category.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        category.setStatus(request.getStatus());

        this.save(category);
    }

    @Override
    public void updateCategory(FoodCategoryUpdateRequest request) {
        FoodCategory category = getRequiredCategory(request.getId());

        String categoryName = normalizeCategoryName(request.getName());
        validateCategoryStatus(request.getStatus());
        validateCategoryNameUnique(categoryName, request.getId());

        category.setName(categoryName);
        category.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        category.setStatus(request.getStatus());

        this.updateById(category);
    }

    @Override
    public void updateCategoryStatus(FoodCategoryUpdateStatusRequest request) {
        FoodCategory category = getRequiredCategory(request.getCategoryId());
        validateCategoryStatus(request.getStatus());

        category.setStatus(request.getStatus());
        this.updateById(category);
    }

    @Override
    public void deleteCategory(Long categoryId) {
        getRequiredCategory(categoryId);

        Long itemCount = foodItemMapper.selectCount(
                new LambdaQueryWrapper<FoodItem>()
                        .eq(FoodItem::getCategoryId, categoryId)
        );
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException(4104, "当前分类下存在菜品，不能删除");
        }

        this.removeById(categoryId);
    }
}