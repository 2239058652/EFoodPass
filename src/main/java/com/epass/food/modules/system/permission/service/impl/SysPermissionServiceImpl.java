package com.epass.food.modules.system.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.modules.system.permission.dto.PermissionCreateRequest;
import com.epass.food.modules.system.permission.dto.PermissionDetailResponse;
import com.epass.food.modules.system.permission.dto.PermissionListQuery;
import com.epass.food.modules.system.permission.dto.PermissionListResponse;
import com.epass.food.modules.system.permission.dto.PermissionUpdateRequest;
import com.epass.food.modules.system.permission.dto.PermissionUpdateStatusRequest;
import com.epass.food.modules.system.permission.entity.SysPermission;
import com.epass.food.modules.system.permission.entity.SysRolePermission;
import com.epass.food.modules.system.permission.mapper.SysPermissionMapper;
import com.epass.food.modules.system.permission.mapper.SysRolePermissionMapper;
import com.epass.food.modules.system.permission.service.SysPermissionService;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.service.SysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SysPermissionServiceImpl implements SysPermissionService {

    private final SysRoleService sysRoleService;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public SysPermissionServiceImpl(SysRoleService sysRoleService,
                                    SysRolePermissionMapper sysRolePermissionMapper,
                                    SysPermissionMapper sysPermissionMapper) {
        this.sysRoleService = sysRoleService;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    private void validatePermissionStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(BizErrorCode.PERMISSION_STATUS_INVALID, "权限状态值不合法");
        }
    }

    private void validatePermissionType(Integer permType) {
        if (!Integer.valueOf(1).equals(permType)
                && !Integer.valueOf(2).equals(permType)
                && !Integer.valueOf(3).equals(permType)) {
            throw new BusinessException(BizErrorCode.PERMISSION_TYPE_INVALID, "权限类型值不合法");
        }
    }

    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        List<SysRole> roleList = sysRoleService.getRolesByUserId(userId);
        if (roleList == null || roleList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = new ArrayList<>();
        for (SysRole role : roleList) {
            roleIds.add(role.getId());
        }

        LambdaQueryWrapper<SysRolePermission> rolePermissionQuery = new LambdaQueryWrapper<>();
        rolePermissionQuery.in(SysRolePermission::getRoleId, roleIds);

        List<SysRolePermission> rolePermissionList = sysRolePermissionMapper.selectList(rolePermissionQuery);
        if (rolePermissionList == null || rolePermissionList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> permissionIds = new ArrayList<>();
        for (SysRolePermission rolePermission : rolePermissionList) {
            permissionIds.add(rolePermission.getPermissionId());
        }

        LambdaQueryWrapper<SysPermission> permissionQuery = new LambdaQueryWrapper<>();
        permissionQuery.in(SysPermission::getId, permissionIds);
        permissionQuery.eq(SysPermission::getStatus, 1);

        List<SysPermission> permissionList = sysPermissionMapper.selectList(permissionQuery);
        if (permissionList == null || permissionList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> permissionCodes = new ArrayList<>();
        for (SysPermission permission : permissionList) {
            permissionCodes.add(permission.getPermCode());
        }
        return permissionCodes;
    }

    @Override
    public PageResult<PermissionListResponse> listPermissions(PermissionListQuery query) {
        if (query == null) {
            query = new PermissionListQuery();
        }

        LambdaQueryWrapper<SysPermission> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getPermCode())) {
            queryWrapper.like(SysPermission::getPermCode, query.getPermCode());
        }

        if (query.getStatus() != null) {
            queryWrapper.eq(SysPermission::getStatus, query.getStatus());
        }

        queryWrapper.orderByDesc(SysPermission::getId);

        Page<SysPermission> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysPermission> permissionPage = sysPermissionMapper.selectPage(page, queryWrapper);

        List<PermissionListResponse> responseList = new ArrayList<>();
        for (SysPermission permission : permissionPage.getRecords()) {
            PermissionListResponse response = new PermissionListResponse();
            response.setId(permission.getId());
            response.setPermCode(permission.getPermCode());
            response.setPermName(permission.getPermName());
            response.setPermType(permission.getPermType());
            response.setPath(permission.getPath());
            response.setMethod(permission.getMethod());
            response.setStatus(permission.getStatus());
            responseList.add(response);
        }

        PageResult<PermissionListResponse> result = new PageResult<>();
        result.setTotal(permissionPage.getTotal());
        result.setPageNum(permissionPage.getCurrent());
        result.setPageSize(permissionPage.getSize());
        result.setRecords(responseList);
        return result;
    }

    @Override
    public void createPermission(PermissionCreateRequest request) {
        Long count = sysPermissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermCode, request.getPermCode())
        );
        if (count != null && count > 0) {
            throw new BusinessException(BizErrorCode.PERMISSION_CODE_EXISTS, "权限编码已存在");
        }

        validatePermissionStatus(request.getStatus());
        validatePermissionType(request.getPermType());

        SysPermission permission = new SysPermission();
        permission.setPermCode(request.getPermCode());
        permission.setPermName(request.getPermName());
        permission.setPermType(request.getPermType());
        permission.setPath(request.getPath());
        permission.setMethod(request.getMethod());
        permission.setStatus(request.getStatus());
        sysPermissionMapper.insert(permission);
    }

    @Override
    public void updatePermissionStatus(PermissionUpdateStatusRequest request) {
        SysPermission permission = sysPermissionMapper.selectById(request.getPermissionId());
        if (permission == null) {
            throw new BusinessException(BizErrorCode.PERMISSION_NOT_FOUND, "权限不存在");
        }

        validatePermissionStatus(request.getStatus());

        if ("admin:dashboard".equals(permission.getPermCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(BizErrorCode.CORE_PERMISSION_CANNOT_DISABLE, "核心权限不能被禁用");
        }

        permission.setStatus(request.getStatus());
        sysPermissionMapper.updateById(permission);
    }

    @Override
    public void deletePermission(Long permissionId) {
        SysPermission permission = sysPermissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException(BizErrorCode.PERMISSION_NOT_FOUND, "权限不存在");
        }

        if ("admin:dashboard".equals(permission.getPermCode())) {
            throw new BusinessException(BizErrorCode.CORE_PERMISSION_CANNOT_DELETE, "核心权限不能被删除");
        }

        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermissionId, permissionId)
        );

        sysPermissionMapper.deleteById(permissionId);
    }

    @Override
    public void updatePermission(PermissionUpdateRequest request) {
        SysPermission permission = sysPermissionMapper.selectById(request.getId());
        if (permission == null) {
            throw new BusinessException(BizErrorCode.PERMISSION_NOT_FOUND, "权限不存在");
        }

        validatePermissionStatus(request.getStatus());
        validatePermissionType(request.getPermType());

        if ("admin:dashboard".equals(permission.getPermCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(BizErrorCode.CORE_PERMISSION_CANNOT_DISABLE, "核心权限不能被禁用");
        }

        permission.setPermName(request.getPermName());
        permission.setPermType(request.getPermType());
        permission.setPath(request.getPath());
        permission.setMethod(request.getMethod());
        permission.setStatus(request.getStatus());
        sysPermissionMapper.updateById(permission);
    }

    @Override
    public PermissionDetailResponse getPermissionDetail(Long permissionId) {
        SysPermission permission = sysPermissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException(BizErrorCode.PERMISSION_NOT_FOUND, "权限不存在");
        }

        PermissionDetailResponse response = new PermissionDetailResponse();
        response.setId(permission.getId());
        response.setPermCode(permission.getPermCode());
        response.setPermName(permission.getPermName());
        response.setPermType(permission.getPermType());
        response.setPath(permission.getPath());
        response.setMethod(permission.getMethod());
        response.setStatus(permission.getStatus());
        return response;
    }
}
