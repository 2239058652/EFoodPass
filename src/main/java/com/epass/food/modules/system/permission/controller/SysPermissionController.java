package com.epass.food.modules.system.permission.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.system.permission.dto.PermissionCreateRequest;
import com.epass.food.modules.system.permission.dto.PermissionListQuery;
import com.epass.food.modules.system.permission.dto.PermissionListResponse;
import com.epass.food.modules.system.permission.service.SysPermissionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/permission")
public class SysPermissionController {

    private final SysPermissionService sysPermissionService;

    public SysPermissionController(SysPermissionService sysPermissionService) {
        this.sysPermissionService = sysPermissionService;
    }

    /**
     * 查询权限列表
     *
     * @param query 查询条件
     * @return 权限列表
     */
    @PreAuthorize("hasAuthority('system:permission:list')")
    @GetMapping("/list")
    public Result<List<PermissionListResponse>> list(PermissionListQuery query) {
        List<PermissionListResponse> permissionList = sysPermissionService.listPermissions(query);
        return Result.success(permissionList);
    }

    /**
     * 新增权限
     *
     * @param request 新增权限请求参数
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:permission:add')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PermissionCreateRequest request) {
        sysPermissionService.createPermission(request);
        return Result.success();
    }
}