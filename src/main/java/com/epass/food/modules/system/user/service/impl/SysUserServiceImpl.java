package com.epass.food.modules.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.entity.SysUserRole;
import com.epass.food.modules.system.role.mapper.SysRoleMapper;
import com.epass.food.modules.system.role.mapper.SysUserRoleMapper;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.dto.UserAssignRoleRequest;
import com.epass.food.modules.system.user.dto.UserCreateRequest;
import com.epass.food.modules.system.user.dto.UserDetailResponse;
import com.epass.food.modules.system.user.dto.UserListQuery;
import com.epass.food.modules.system.user.dto.UserListResponse;
import com.epass.food.modules.system.user.dto.UserResetPasswordRequest;
import com.epass.food.modules.system.user.dto.UserUpdateRequest;
import com.epass.food.modules.system.user.dto.UserUpdateStatusRequest;
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

    private void validateUserStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(BizErrorCode.USER_STATUS_INVALID, "用户状态值不合法");
        }
    }

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        queryWrapper.last("limit 1");
        return this.getOne(queryWrapper);
    }

    @Override
    public PageResult<UserListResponse> listUsers(UserListQuery query) {
        if (query == null) {
            query = new UserListQuery();
        }

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getUsername())) {
            queryWrapper.like(SysUser::getUsername, query.getUsername());
        }

        if (query.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, query.getStatus());
        }

        queryWrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysUser> userPage = this.page(page, queryWrapper);
        List<SysUser> userList = userPage.getRecords();

        List<UserListResponse> responseList = new ArrayList<>();
        for (SysUser user : userList) {
            List<SysRole> roleList = sysRoleService.getRolesByUserId(user.getId());
            responseList.add(getUserListResponse(user, roleList));
        }

        PageResult<UserListResponse> result = new PageResult<>();
        result.setTotal(userPage.getTotal());
        result.setPageNum(userPage.getCurrent());
        result.setPageSize(userPage.getSize());
        result.setRecords(responseList);
        return result;
    }

    @Override
    public void createUser(UserCreateRequest request) {
        SysUser existUser = this.getByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(BizErrorCode.USERNAME_EXISTS, "用户名已存在");
        }

        validateUserStatus(request.getStatus());

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        user.setTokenVersion(0);

        this.save(user);
    }

    @Override
    public void assignRoles(UserAssignRoleRequest request) {
        SysUser user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        Long roleCount = sysRoleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, request.getRoleIds())
                        .eq(SysRole::getStatus, 1)
        );
        if (roleCount == null || roleCount != request.getRoleIds().size()) {
            throw new BusinessException(BizErrorCode.USER_ROLE_NOT_FOUND_OR_DISABLED, "角色不存在或已禁用");
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

    @Override
    public void updateUserStatus(UserUpdateStatusRequest request) {
        SysUser user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        validateUserStatus(request.getStatus());

        if ("admin".equals(user.getUsername()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(BizErrorCode.ADMIN_USER_CANNOT_DISABLE, "系统管理员不能被禁用");
        }

        user.setStatus(request.getStatus());
        this.updateById(user);
    }

    @Override
    public void deleteUser(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        if ("admin".equals(user.getUsername())) {
            throw new BusinessException(BizErrorCode.ADMIN_USER_CANNOT_DELETE, "系统管理员不能被删除");
        }

        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        this.removeById(userId);
    }

    @Override
    public void updateUser(UserUpdateRequest request) {
        SysUser user = this.getById(request.getId());
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        validateUserStatus(request.getStatus());

        if ("admin".equals(user.getUsername()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(BizErrorCode.ADMIN_USER_CANNOT_DISABLE, "系统管理员不能被禁用");
        }

        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        this.updateById(user);
    }

    @Override
    public void resetPassword(UserResetPasswordRequest request) {
        SysUser user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        int oldVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        user.setTokenVersion(oldVersion + 1);
        this.updateById(user);
    }

    @Override
    public UserDetailResponse getUserDetail(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
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
