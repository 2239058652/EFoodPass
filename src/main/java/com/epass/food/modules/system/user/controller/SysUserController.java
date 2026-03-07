package com.epass.food.modules.system.user.controller;

import com.epass.food.common.result.Result;
import com.epass.food.modules.system.user.dto.*;
import com.epass.food.modules.system.user.service.SysUserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @return 用户列表
     */
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/list")
    public Result<List<UserListResponse>> list(UserListQuery query) {
        List<UserListResponse> userList = sysUserService.listUsers(query);
        return Result.success(userList);
    }

    /**
     * 新增用户
     *
     * @param request 新增用户请求参数
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:user:add')")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateRequest request) {
        sysUserService.createUser(request);
        return Result.success();
    }

    /**
     * 给用户分配角色
     *
     * @param request 分配角色请求参数
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:user:assign-role')")
    @PostMapping("/assign-role")
    public Result<Void> assignRole(@Valid @RequestBody UserAssignRoleRequest request) {
        sysUserService.assignRoles(request);
        return Result.success();
    }

    /**
     * 更新用户状态
     *
     * @param request 更新用户状态请求参数
     * @return 无
     */
    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody UserUpdateStatusRequest request) {
        sysUserService.updateUserStatus(request);
        return Result.success();
    }
}