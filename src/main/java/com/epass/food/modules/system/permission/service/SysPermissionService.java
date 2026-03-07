package com.epass.food.modules.system.permission.service;

import com.epass.food.modules.system.permission.dto.PermissionCreateRequest;
import com.epass.food.modules.system.permission.dto.PermissionListQuery;
import com.epass.food.modules.system.permission.dto.PermissionListResponse;

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
    List<PermissionListResponse> listPermissions(PermissionListQuery query);

    /**
     * 新增权限
     *
     * @param request 新增权限请求参数
     */
    void createPermission(PermissionCreateRequest request);
}