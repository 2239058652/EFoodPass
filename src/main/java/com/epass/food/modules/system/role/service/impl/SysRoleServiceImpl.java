package com.epass.food.modules.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.system.permission.entity.SysPermission;
import com.epass.food.modules.system.permission.entity.SysRolePermission;
import com.epass.food.modules.system.permission.mapper.SysPermissionMapper;
import com.epass.food.modules.system.permission.mapper.SysRolePermissionMapper;
import com.epass.food.modules.system.role.dto.*;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.entity.SysUserRole;
import com.epass.food.modules.system.role.mapper.SysRoleMapper;
import com.epass.food.modules.system.role.mapper.SysUserRoleMapper;
import com.epass.food.modules.system.role.service.SysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public SysRoleServiceImpl(SysUserRoleMapper sysUserRoleMapper,
                              SysRolePermissionMapper sysRolePermissionMapper,
                              SysPermissionMapper sysPermissionMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    /**
     * 根据用户ID查询用户角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRole> getRolesByUserId(Long userId) {
        LambdaQueryWrapper<SysUserRole> userRoleQuery = new LambdaQueryWrapper<>();
        userRoleQuery.eq(SysUserRole::getUserId, userId);

        List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(userRoleQuery);
        if (userRoleList == null || userRoleList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoleList.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<SysRole> roleQuery = new LambdaQueryWrapper<>();
        roleQuery.in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, 1);

        return this.list(roleQuery);
    }

    /**
     * 查询角色列表
     *
     * @param query 查询条件
     * @return 角色列表
     */
    @Override
    public List<RoleListResponse> listRoles(RoleListQuery query) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();

        if (query != null && StringUtils.hasText(query.getRoleCode())) {
            queryWrapper.like(SysRole::getRoleCode, query.getRoleCode());
        }

        if (query != null && query.getStatus() != null) {
            queryWrapper.eq(SysRole::getStatus, query.getStatus());
        }

        queryWrapper.orderByDesc(SysRole::getId);

        List<SysRole> roleList = this.list(queryWrapper);

        List<RoleListResponse> responseList = new ArrayList<>();
        for (SysRole role : roleList) {
            RoleListResponse response = new RoleListResponse();
            response.setId(role.getId());
            response.setRoleCode(role.getRoleCode());
            response.setRoleName(role.getRoleName());
            response.setStatus(role.getStatus());
            responseList.add(response);
        }

        return responseList;
    }

    /**
     * 新增角色
     *
     * @param request 新增角色请求参数
     */
    @Override
    public void createRole(RoleCreateRequest request) {
        Long count = this.count(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, request.getRoleCode())
        );
        if (count != null && count > 0) {
            throw new BusinessException(4006, "角色编码已存在");
        }

        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus());

        this.save(role);
    }

    /**
     * 分配权限
     *
     * @param request 分配权限请求参数
     */
    @Override
    public void assignPermissions(RoleAssignPermissionRequest request) {
        SysRole role = this.getById(request.getRoleId());
        if (role == null) {
            throw new BusinessException(4007, "角色不存在");
        }

        Long permissionCount = sysPermissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, request.getPermissionIds())
                        .eq(SysPermission::getStatus, 1)
        );
        if (permissionCount == null || permissionCount != request.getPermissionIds().size()) {
            throw new BusinessException(4008, "权限不存在或已禁用");
        }

        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, request.getRoleId())
        );

        for (Long permissionId : request.getPermissionIds()) {
            SysRolePermission rolePermission = new SysRolePermission();
            rolePermission.setRoleId(request.getRoleId());
            rolePermission.setPermissionId(permissionId);
            sysRolePermissionMapper.insert(rolePermission);
        }
    }

    /**
     * 更新角色状态
     *
     * @param request 更新角色状态请求参数
     */
    @Override
    public void updateRoleStatus(RoleUpdateStatusRequest request) {
        SysRole role = this.getById(request.getRoleId());
        if (role == null) {
            throw new BusinessException(4007, "角色不存在");
        }

        if (!Integer.valueOf(0).equals(request.getStatus()) && !Integer.valueOf(1).equals(request.getStatus())) {
            throw new BusinessException(4012, "角色状态值不合法");
        }

        if ("ADMIN".equals(role.getRoleCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(4013, "系统管理员角色不能被禁用");
        }

        role.setStatus(request.getStatus());
        this.updateById(role);
    }

    /**
     * 删除角色
     *
     * @param roleId 角色ID
     */
    @Override
    public void deleteRole(Long roleId) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(4007, "角色不存在");
        }

        if ("ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException(4017, "系统管理员角色不能被删除");
        }

        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getRoleId, roleId)
        );

        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId)
        );

        this.removeById(roleId);
    }

    /**
     * 获取角色详情
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    @Override
    public RoleDetailResponse getRoleDetail(Long roleId) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(4007, "角色不存在");
        }

        List<SysRolePermission> rolePermissionList = sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getRoleId, roleId)
        );

        List<Long> permissionIds = new ArrayList<>();
        for (SysRolePermission rolePermission : rolePermissionList) {
            permissionIds.add(rolePermission.getPermissionId());
        }

        RoleDetailResponse response = new RoleDetailResponse();
        response.setId(role.getId());
        response.setRoleCode(role.getRoleCode());
        response.setRoleName(role.getRoleName());
        response.setStatus(role.getStatus());
        response.setPermissionIds(permissionIds);

        return response;
    }

    /**
     * 修改角色基础信息
     *
     * @param request 修改角色请求参数
     */
    @Override
    public void updateRole(RoleUpdateRequest request) {
        SysRole role = this.getById(request.getId());
        if (role == null) {
            throw new BusinessException(4007, "角色不存在");
        }

        if (!Integer.valueOf(0).equals(request.getStatus()) && !Integer.valueOf(1).equals(request.getStatus())) {
            throw new BusinessException(4012, "角色状态值不合法");
        }

        if ("ADMIN".equals(role.getRoleCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(4013, "系统管理员角色不能被禁用");
        }

        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus());

        this.updateById(role);
    }

}
