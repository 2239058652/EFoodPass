package com.epass.food.modules.system.role.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.system.role.dto.RoleAssignPermissionRequest;
import com.epass.food.modules.system.role.dto.RoleCreateRequest;
import com.epass.food.modules.system.role.dto.RoleListQuery;
import com.epass.food.modules.system.role.dto.RoleListResponse;
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
}