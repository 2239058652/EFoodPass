package com.epass.food.modules.food.category.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.category.dto.*;
import com.epass.food.modules.food.category.entity.FoodCategory;

public interface FoodCategoryService extends IService<FoodCategory> {

    /**
     * 分页查询分类列表
     *
     * @param query 查询条件
     * @return 分类分页结果
     */
    PageResult<FoodCategoryListResponse> listCategories(FoodCategoryListQuery query);

    /**
     * 查询分类详情
     *
     * @param categoryId 分类ID
     * @return 分类详情
     */
    FoodCategoryDetailResponse getCategoryDetail(Long categoryId);

    /**
     * 新增分类
     *
     * @param request 新增请求参数
     */
    void createCategory(FoodCategoryCreateRequest request);

    /**
     * 修改分类
     *
     * @param request 修改请求参数
     */
    void updateCategory(FoodCategoryUpdateRequest request);

    /**
     * 修改分类状态
     *
     * @param request 修改状态请求参数
     */
    void updateCategoryStatus(FoodCategoryUpdateStatusRequest request);

    /**
     * 删除分类
     *
     * @param categoryId 分类ID
     */
    void deleteCategory(Long categoryId);
}