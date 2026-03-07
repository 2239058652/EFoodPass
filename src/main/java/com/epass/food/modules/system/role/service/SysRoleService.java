package com.epass.food.modules.system.role.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.modules.system.role.dto.*;
import com.epass.food.modules.system.role.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    /**
     * 根据用户 ID 查询角色列表
     *
     * @param userId 用户 ID
     * @return 角色列表
     */
    List<SysRole> getRolesByUserId(Long userId);

    /**
     * 查询角色列表
     *
     * @param query 查询条件
     * @return 角色列表
     */
    List<RoleListResponse> listRoles(RoleListQuery query);

    /**
     * 新增角色
     *
     * @param request 新增角色请求参数
     */
    void createRole(RoleCreateRequest request);

    /**
     * 给角色分配权限
     *
     * @param request 分配权限请求参数
     */
    void assignPermissions(RoleAssignPermissionRequest request);

    /**
     * 修改角色状态
     *
     * @param request 修改角色状态请求参数
     */
    void updateRoleStatus(RoleUpdateStatusRequest request);
}
