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

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    @PreAuthorize("hasAuthority('system:user:delete')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        sysUserService.deleteUser(id);
        return Result.success();
    }

    /**
     * 更新用户
     *
     * @param request 更新用户请求参数
     */
    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody UserUpdateRequest request) {
        sysUserService.updateUser(request);
        return Result.success();
    }

    /**
     * 重置用户密码
     *
     * @param request 重置密码请求参数
     */
    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody UserResetPasswordRequest request) {
        sysUserService.resetPassword(request);
        return Result.success();
    }

    /**
     * 获取用户详情 / 角色回显接口
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/{id}")
    public Result<UserDetailResponse> detail(@PathVariable Long id) {
        UserDetailResponse response = sysUserService.getUserDetail(id);
        return Result.success(response);
    }

}