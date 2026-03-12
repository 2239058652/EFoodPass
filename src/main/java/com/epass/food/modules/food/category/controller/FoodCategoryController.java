package com.epass.food.modules.food.category.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.food.category.dto.*;
import com.epass.food.modules.food.category.service.FoodCategoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/food/category")
public class FoodCategoryController {

    private final FoodCategoryService foodCategoryService;

    public FoodCategoryController(FoodCategoryService foodCategoryService) {
        this.foodCategoryService = foodCategoryService;
    }

    /**
     * 分页查询分类列表
     *
     * @param query 查询条件
     * @return 分类分页结果
     */
    @PreAuthorize("hasAuthority('food:category:list')")
    @GetMapping("/list")
    public Result<PageResult<FoodCategoryListResponse>> list(FoodCategoryListQuery query) {
        PageResult<FoodCategoryListResponse> pageResult = foodCategoryService.listCategories(query);
        return Result.success(pageResult);
    }

    /**
     * 查询分类详情
     *
     * @param id 分类ID
     * @return 分类详情
     */
    @PreAuthorize("hasAuthority('food:category:detail')")
    @GetMapping("/{id}")
    public Result<FoodCategoryDetailResponse> detail(@PathVariable Long id) {
        FoodCategoryDetailResponse response = foodCategoryService.getCategoryDetail(id);
        return Result.success(response);
    }

    /**
     * 新增分类
     *
     * @param request 新增请求参数
     * @return 结果
     */
    @PreAuthorize("hasAuthority('food:category:add')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody FoodCategoryCreateRequest request) {
        foodCategoryService.createCategory(request);
        return Result.success();
    }

    /**
     * 修改分类
     *
     * @param request 修改请求参数
     * @return 结果
     */
    @PreAuthorize("hasAuthority('food:category:update')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody FoodCategoryUpdateRequest request) {
        foodCategoryService.updateCategory(request);
        return Result.success();
    }

    /**
     * 修改分类状态
     *
     * @param request 修改状态请求参数
     * @return 结果
     */
    @PreAuthorize("hasAuthority('food:category:update-status')")
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody FoodCategoryUpdateStatusRequest request) {
        foodCategoryService.updateCategoryStatus(request);
        return Result.success();
    }

    /**
     * 删除分类
     *
     * @param id 分类ID
     * @return 结果
     */
    @PreAuthorize("hasAuthority('food:category:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        foodCategoryService.deleteCategory(id);
        return Result.success();
    }
}
