package com.epass.food.modules.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.system.permission.entity.SysPermission;
import com.epass.food.modules.system.permission.entity.SysRolePermission;
import com.epass.food.modules.system.permission.mapper.SysPermissionMapper;
import com.epass.food.modules.system.permission.mapper.SysRolePermissionMapper;
import com.epass.food.modules.system.role.dto.RoleAssignPermissionRequest;
import com.epass.food.modules.system.role.dto.RoleCreateRequest;
import com.epass.food.modules.system.role.dto.RoleDetailResponse;
import com.epass.food.modules.system.role.dto.RoleListQuery;
import com.epass.food.modules.system.role.dto.RoleListResponse;
import com.epass.food.modules.system.role.dto.RoleUpdateRequest;
import com.epass.food.modules.system.role.dto.RoleUpdateStatusRequest;
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

    private void validateRoleStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(BizErrorCode.ROLE_STATUS_INVALID, "角色状态值不合法");
        }
    }

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

    @Override
    public PageResult<RoleListResponse> listRoles(RoleListQuery query) {
        if (query == null) {
            query = new RoleListQuery();
        }

        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getRoleCode())) {
            queryWrapper.like(SysRole::getRoleCode, query.getRoleCode());
        }

        if (query.getStatus() != null) {
            queryWrapper.eq(SysRole::getStatus, query.getStatus());
        }

        queryWrapper.orderByDesc(SysRole::getId);

        Page<SysRole> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysRole> rolePage = this.page(page, queryWrapper);

        List<RoleListResponse> responseList = new ArrayList<>();
        for (SysRole role : rolePage.getRecords()) {
            RoleListResponse response = new RoleListResponse();
            response.setId(role.getId());
            response.setRoleCode(role.getRoleCode());
            response.setRoleName(role.getRoleName());
            response.setStatus(role.getStatus());
            responseList.add(response);
        }

        PageResult<RoleListResponse> result = new PageResult<>();
        result.setTotal(rolePage.getTotal());
        result.setPageNum(rolePage.getCurrent());
        result.setPageSize(rolePage.getSize());
        result.setRecords(responseList);
        return result;
    }

    @Override
    public void createRole(RoleCreateRequest request) {
        long count = this.count(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, request.getRoleCode())
        );
        if (count > 0) {
            throw new BusinessException(BizErrorCode.ROLE_CODE_EXISTS, "角色编码已存在");
        }

        validateRoleStatus(request.getStatus());

        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus());
        this.save(role);
    }

    @Override
    public void assignPermissions(RoleAssignPermissionRequest request) {
        SysRole role = this.getById(request.getRoleId());
        if (role == null) {
            throw new BusinessException(BizErrorCode.ROLE_NOT_FOUND, "角色不存在");
        }

        Long permissionCount = sysPermissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, request.getPermissionIds())
                        .eq(SysPermission::getStatus, 1)
        );
        if (permissionCount == null || permissionCount != request.getPermissionIds().size()) {
            throw new BusinessException(BizErrorCode.PERMISSION_NOT_FOUND, "权限不存在或已禁用");
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

    @Override
    public void updateRoleStatus(RoleUpdateStatusRequest request) {
        SysRole role = this.getById(request.getRoleId());
        if (role == null) {
            throw new BusinessException(BizErrorCode.ROLE_NOT_FOUND, "角色不存在");
        }

        validateRoleStatus(request.getStatus());

        if ("ADMIN".equals(role.getRoleCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(BizErrorCode.ADMIN_ROLE_CANNOT_DISABLE, "系统管理员角色不能被禁用");
        }

        role.setStatus(request.getStatus());
        this.updateById(role);
    }

    @Override
    public void deleteRole(Long roleId) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(BizErrorCode.ROLE_NOT_FOUND, "角色不存在");
        }

        if ("ADMIN".equals(role.getRoleCode())) {
            throw new BusinessException(BizErrorCode.ADMIN_ROLE_CANNOT_DELETE, "系统管理员角色不能被删除");
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

    @Override
    public RoleDetailResponse getRoleDetail(Long roleId) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new BusinessException(BizErrorCode.ROLE_NOT_FOUND, "角色不存在");
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

    @Override
    public void updateRole(RoleUpdateRequest request) {
        SysRole role = this.getById(request.getId());
        if (role == null) {
            throw new BusinessException(BizErrorCode.ROLE_NOT_FOUND, "角色不存在");
        }

        validateRoleStatus(request.getStatus());

        if ("ADMIN".equals(role.getRoleCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(BizErrorCode.ADMIN_ROLE_CANNOT_DISABLE, "系统管理员角色不能被禁用");
        }

        role.setRoleName(request.getRoleName());
        role.setStatus(request.getStatus());
        this.updateById(role);
    }
}
