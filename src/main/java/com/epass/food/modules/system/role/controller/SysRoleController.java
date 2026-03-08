package com.epass.food.modules.system.role.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.system.role.dto.*;
import com.epass.food.modules.system.role.service.SysRoleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    /**
     * 查询角色列表
     *
     * @param query 查询条件
     * @return 角色列表
     */
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/list")
    public Result<List<RoleListResponse>> list(RoleListQuery query) {
        List<RoleListResponse> roleList = sysRoleService.listRoles(query);
        return Result.success(roleList);
    }

    /**
     * 创建角色
     *
     * @param request 新增角色请求参数
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:role:add')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody RoleCreateRequest request) {
        sysRoleService.createRole(request);
        return Result.success();
    }

    /**
     * 角色分配权限
     *
     * @param request 角色分配权限请求参数
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:role:assign-permission')")
    @PostMapping("/assign-permission")
    public Result<Void> assignPermission(@Valid @RequestBody RoleAssignPermissionRequest request) {
        sysRoleService.assignPermissions(request);
        return Result.success();
    }

    /**
     * 更新角色状态
     *
     * @param request 更新角色状态请求参数
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:role:update')")
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody RoleUpdateStatusRequest request) {
        sysRoleService.updateRoleStatus(request);
        return Result.success();
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:role:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysRoleService.deleteRole(id);
        return Result.success();
    }

    /**
     * 角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/{id}")
    public Result<RoleDetailResponse> detail(@PathVariable Long id) {
        RoleDetailResponse response = sysRoleService.getRoleDetail(id);
        return Result.success(response);
    }

    /**
     * 修改角色基础信息
     *
     * @param request 更新角色请求参数
     */
    @PreAuthorize("hasAuthority('system:role:update')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody RoleUpdateRequest request) {
        sysRoleService.updateRole(request);
        return Result.success();
    }

}