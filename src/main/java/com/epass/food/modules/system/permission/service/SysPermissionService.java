package com.epass.food.modules.system.permission.service;

import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.permission.dto.*;

import java.util.List;

public interface SysPermissionService {

    /**
     * 根据用户ID查询用户权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> getPermissionCodesByUserId(Long userId);

    /**
     * 查询权限列表
     *
     * @param query 查询条件
     * @return 权限列表
     */
    PageResult<PermissionListResponse> listPermissions(PermissionListQuery query);

    /**
     * 新增权限
     *
     * @param request 新增权限请求参数
     */
    void createPermission(PermissionCreateRequest request);

    /**
     * 修改权限状态
     *
     * @param request 修改权限状态请求参数
     */
    void updatePermissionStatus(PermissionUpdateStatusRequest request);

    /**
     * 删除权限
     *
     * @param permissionId 权限ID
     */
    void deletePermission(Long permissionId);

    /**
     * 修改权限基础信息
     *
     * @param request 修改权限请求参数
     */
    void updatePermission(PermissionUpdateRequest request);

    /**
     * 查询权限详情
     *
     * @param permissionId 权限ID
     * @return 权限详情
     */
    PermissionDetailResponse getPermissionDetail(Long permissionId);

}