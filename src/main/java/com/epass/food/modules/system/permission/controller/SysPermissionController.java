package com.epass.food.modules.system.permission.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.system.permission.dto.*;
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
     */
    @PreAuthorize("hasAuthority('system:permission:add')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody PermissionCreateRequest request) {
        sysPermissionService.createPermission(request);
        return Result.success();
    }

    /**
     * 更新权限状态
     *
     * @param request 更新权限状态请求参数
     */
    @PreAuthorize("hasAuthority('system:permission:update')")
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody PermissionUpdateStatusRequest request) {
        sysPermissionService.updatePermissionStatus(request);
        return Result.success();
    }

    /**
     * 删除权限
     *
     * @param id 权限ID
     */
    @PreAuthorize("hasAuthority('system:permission:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysPermissionService.deletePermission(id);
        return Result.success();
    }

    /**
     * 修改权限基础信息
     *
     * @param request 修改权限请求参数
     */
    @PreAuthorize("hasAuthority('system:permission:update')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody PermissionUpdateRequest request) {
        sysPermissionService.updatePermission(request);
        return Result.success();
    }

    /**
     * 查询权限详情
     *
     * @param id 权限ID
     * @return 权限详情
     */
    @PreAuthorize("hasAuthority('system:permission:list')")
    @GetMapping("/{id}")
    public Result<PermissionDetailResponse> detail(@PathVariable("id") Long id) {
        PermissionDetailResponse response = sysPermissionService.getPermissionDetail(id);
        return Result.success(response);
    }

}