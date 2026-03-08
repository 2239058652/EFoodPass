package com.epass.food.modules.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.entity.SysUserRole;
import com.epass.food.modules.system.role.mapper.SysRoleMapper;
import com.epass.food.modules.system.role.mapper.SysUserRoleMapper;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.dto.*;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.mapper.SysUserMapper;
import com.epass.food.modules.system.user.service.SysUserService;
import lombok.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleService sysRoleService;
    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    public SysUserServiceImpl(SysRoleService sysRoleService,
                              PasswordEncoder passwordEncoder,
                              SysUserRoleMapper sysUserRoleMapper,
                              SysRoleMapper sysRoleMapper) {
        this.sysRoleService = sysRoleService;
        this.passwordEncoder = passwordEncoder;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

    /**
     * 内部私有方法
     * 获取用户列表响应对象
     *
     * @param user     用户对象
     * @param roleList 角色列表
     * @return 用户列表响应对象
     */
    private static @NonNull UserListResponse getUserListResponse(SysUser user, List<SysRole> roleList) {
        List<String> roleCodes = new ArrayList<>();
        for (SysRole role : roleList) {
            roleCodes.add(role.getRoleCode());
        }

        UserListResponse response = new UserListResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setRoleCodes(roleCodes);
        return response;
    }

    /**
     * 根据用户名查询系统用户信息
     *
     * @param username 用户名，用于查询的唯一标识
     * @return SysUser 匹配的用户对象，如果未找到则返回 null
     * SELECT * FROM sys_user WHERE username = '你传入的用户名' LIMIT 1;
     */
    @Override
    public SysUser getByUsername(String username) {
        // 1. 创建一个针对 SysUser 实体类的 Lambda 条件构造器
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        // 2. 拼接 SQL 里的 WHERE 条件
        // 相当于生成 SQL: WHERE username = 传入的参数值
        queryWrapper.eq(SysUser::getUsername, username);

        // 3. 在生成的 SQL 语句最后面强行加上 "limit 1"
        // 相当于生成 SQL: WHERE username = ? limit 1
        queryWrapper.last("limit 1");

        // 4. 执行查询，并返回查询到的单个对象
        return this.getOne(queryWrapper);
    }

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @return 用户列表
     */
    @Override
    public PageResult<UserListResponse> listUsers(UserListQuery query) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (query != null && StringUtils.hasText(query.getUsername())) {
            queryWrapper.like(SysUser::getUsername, query.getUsername());  // 用户名模糊查询
        }

        if (query != null && query.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, query.getStatus()); // 按前端传的状态筛选
        }

        queryWrapper.orderByDesc(SysUser::getId); // 按 ID 倒序排列（最新数据在前面）

        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysUser> userPage = this.page(page, queryWrapper); // 分页查询
        List<SysUser> userList = userPage.getRecords(); // 取出当前页的数据列表

        List<UserListResponse> responseList = new ArrayList<>();
        for (SysUser user : userList) {
            List<SysRole> roleList = sysRoleService.getRolesByUserId(user.getId());

            UserListResponse response = getUserListResponse(user, roleList);

            responseList.add(response);
        }

        PageResult<UserListResponse> result = new PageResult<>();
        result.setTotal(userPage.getTotal());
        result.setPageNum(userPage.getCurrent());
        result.setPageSize(userPage.getSize());
        result.setRecords(responseList);

        return result;
    }

    /**
     * 创建用户
     *
     * @param request 创建用户的请求参数
     */
    @Override
    public void createUser(UserCreateRequest request) {
        SysUser existUser = this.getByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(4001, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        user.setTokenVersion(0);

        this.save(user);
    }

    /**
     * 分配角色
     *
     * @param request 分配角色的请求参数
     */
    @Override
    public void assignRoles(UserAssignRoleRequest request) {
        SysUser user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        Long roleCount = sysRoleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, request.getRoleIds())
                        .eq(SysRole::getStatus, 1)
        );
        if (roleCount == null || roleCount != request.getRoleIds().size()) {
            throw new BusinessException(4005, "角色不存在或已禁用");
        }

        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, request.getUserId())
        );

        for (Long roleId : request.getRoleIds()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(request.getUserId());
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    /**
     * 更新用户状态
     *
     * @param request 更新用户状态的请求参数
     */
    @Override
    public void updateUserStatus(UserUpdateStatusRequest request) {
        SysUser user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        if (!Integer.valueOf(0).equals(request.getStatus()) && !Integer.valueOf(1).equals(request.getStatus())) {
            throw new BusinessException(4010, "用户状态值不合法");
        }

        if ("admin".equals(user.getUsername()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(4011, "系统管理员不能被禁用");
        }

        user.setStatus(request.getStatus());
        this.updateById(user);
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    @Override
    public void deleteUser(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        if ("admin".equals(user.getUsername())) {
            throw new BusinessException(4016, "系统管理员不能被删除");
        }

        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        this.removeById(userId);
    }

    /**
     * 修改用户信息
     *
     * @param request 修改用户信息的请求参数
     */
    @Override
    public void updateUser(UserUpdateRequest request) {
        SysUser user = this.getById(request.getId());
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        if (!Integer.valueOf(0).equals(request.getStatus()) && !Integer.valueOf(1).equals(request.getStatus())) {
            throw new BusinessException(4010, "用户状态值不合法");
        }

        if ("admin".equals(user.getUsername()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(4011, "系统管理员不能被禁用");
        }

        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());

        this.updateById(user);
    }

    /**
     * 重置密码
     * 旧的token 已失效
     *
     * @param request 重置密码的请求参数
     */
    @Override
    public void resetPassword(UserResetPasswordRequest request) {
        SysUser user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        Integer oldVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(oldVersion + 1);

        this.updateById(user);
    }

    /**
     * 获取用户详情
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    @Override
    public UserDetailResponse getUserDetail(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        List<Long> roleIds = new ArrayList<>();
        for (SysUserRole userRole : userRoleList) {
            roleIds.add(userRole.getRoleId());
        }

        UserDetailResponse response = new UserDetailResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setRoleIds(roleIds);

        return response;
    }

}
