package com.epass.food.modules.system.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.modules.system.user.dto.*;
import com.epass.food.modules.system.user.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {

    // 根据用户名查询用户
    SysUser getByUsername(String username);

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @return 用户列表
     */
    List<UserListResponse> listUsers(UserListQuery query);

    /**
     * 创建用户
     *
     * @param request 新增用户请求参数
     */
    void createUser(UserCreateRequest request);

    /**
     * 给用户分配角色
     *
     * @param request 分配角色请求参数
     */
    void assignRoles(UserAssignRoleRequest request);

    /**
     * 修改用户状态
     *
     * @param request 修改用户状态请求参数
     */
    void updateUserStatus(UserUpdateStatusRequest request);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

    /**
     * 修改用户基础信息
     *
     * @param request 修改用户请求参数
     */
    void updateUser(UserUpdateRequest request);

    /**
     * 重置用户密码
     *
     * @param request 重置密码请求参数
     */
    void resetPassword(UserResetPasswordRequest request);

    /**
     * 查询用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    UserDetailResponse getUserDetail(Long userId);

}
