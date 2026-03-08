package com.epass.food.modules.system.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.system.permission.dto.*;
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

    /**
     * 根据用户 id 查询权限列表
     *
     * @param userId 用户 id
     * @return 权限列表
     */
    @Override
    public List<String> getPermissionCodesByUserId(Long userId) {
        // 1. 先看这个用户有哪些角色
        // 2. 再看这些角色关联了哪些权限
        // 3. 再查这些权限到底是什么
        // 4. 最后把权限编码列表返回出去
        // 最终你拿到的是：["admin:dashboard"]

        // 根据用户 id，查这个用户有哪些角色
        List<SysRole> roleList = sysRoleService.getRolesByUserId(userId);
        if (roleList == null || roleList.isEmpty()) {
            return Collections.emptyList();
        }

        // 把角色对象变成角色 id 列表
        List<Long> roleIds = new ArrayList<>();
        for (SysRole role : roleList) {
            roleIds.add(role.getId());
        }

        // 根据角色 id 列表查角色权限关联表
        LambdaQueryWrapper<SysRolePermission> rolePermissionQuery = new LambdaQueryWrapper<>();
        rolePermissionQuery.in(SysRolePermission::getRoleId, roleIds); // 查这些角色分别绑定了哪些权限

        List<SysRolePermission> rolePermissionList = sysRolePermissionMapper.selectList(rolePermissionQuery);
        if (rolePermissionList == null || rolePermissionList.isEmpty()) {
            return Collections.emptyList();
        }

        // 从角色权限关联里提取权限 id 列表
        List<Long> permissionIds = new ArrayList<>();
        for (SysRolePermission rolePermission : rolePermissionList) {
            permissionIds.add(rolePermission.getPermissionId());
        }

        // 根据权限 id 列表，查权限表
        LambdaQueryWrapper<SysPermission> permissionQuery = new LambdaQueryWrapper<>();
        permissionQuery.in(SysPermission::getId, permissionIds); // where id in (这些权限id)
        permissionQuery.eq(SysPermission::getStatus, 1); // where status = 1 查启用状态的权限

        List<SysPermission> permissionList = sysPermissionMapper.selectList(permissionQuery);
        if (permissionList == null || permissionList.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取权限列表的权限码
        List<String> permissionCodes = new ArrayList<>();
        for (SysPermission permission : permissionList) {
            permissionCodes.add(permission.getPermCode());
        }

        return permissionCodes;
    }

    @Override
    public List<PermissionListResponse> listPermissions(PermissionListQuery query) {
        LambdaQueryWrapper<SysPermission> queryWrapper = new LambdaQueryWrapper<>();

        if (query != null && StringUtils.hasText(query.getPermCode())) {
            queryWrapper.like(SysPermission::getPermCode, query.getPermCode());
        }

        if (query != null && query.getStatus() != null) {
            queryWrapper.eq(SysPermission::getStatus, query.getStatus());
        }

        queryWrapper.orderByDesc(SysPermission::getId);

        List<SysPermission> permissionList = sysPermissionMapper.selectList(queryWrapper);

        List<PermissionListResponse> responseList = new ArrayList<>();
        for (SysPermission permission : permissionList) {
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

        return responseList;
    }

    /**
     * 新增权限
     *
     * @param request 新增权限请求参数
     */
    @Override
    public void createPermission(PermissionCreateRequest request) {
        Long count = sysPermissionMapper.selectCount(
                new LambdaQueryWrapper<SysPermission>()
                        .eq(SysPermission::getPermCode, request.getPermCode())
        );
        if (count != null && count > 0) {
            throw new BusinessException(4009, "权限编码已存在");
        }

        SysPermission permission = new SysPermission();
        permission.setPermCode(request.getPermCode());
        permission.setPermName(request.getPermName());
        permission.setPermType(request.getPermType());
        permission.setPath(request.getPath());
        permission.setMethod(request.getMethod());
        permission.setStatus(request.getStatus());

        sysPermissionMapper.insert(permission);
    }

    /**
     * 修改权限状态
     *
     * @param request 修改权限状态请求参数
     */
    @Override
    public void updatePermissionStatus(PermissionUpdateStatusRequest request) {
        SysPermission permission = sysPermissionMapper.selectById(request.getPermissionId());
        if (permission == null) {
            throw new BusinessException(4008, "权限不存在");
        }

        if (!Integer.valueOf(0).equals(request.getStatus()) && !Integer.valueOf(1).equals(request.getStatus())) {
            throw new BusinessException(4014, "权限状态值不合法");
        }

        if ("admin:dashboard".equals(permission.getPermCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(4015, "核心权限不能被禁用");
        }

        permission.setStatus(request.getStatus());
        sysPermissionMapper.updateById(permission);
    }

    /**
     * 删除权限
     *
     * @param permissionId 权限ID
     */
    @Override
    public void deletePermission(Long permissionId) {
        SysPermission permission = sysPermissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException(4008, "权限不存在");
        }

        if ("admin:dashboard".equals(permission.getPermCode())) {
            throw new BusinessException(4018, "核心权限不能被删除");
        }

        sysRolePermissionMapper.delete(
                new LambdaQueryWrapper<SysRolePermission>()
                        .eq(SysRolePermission::getPermissionId, permissionId)
        );

        sysPermissionMapper.deleteById(permissionId);
    }

    /**
     * 修改权限基础信息
     *
     * @param request 修改权限请求参数
     */
    @Override
    public void updatePermission(PermissionUpdateRequest request) {
        SysPermission permission = sysPermissionMapper.selectById(request.getId());
        if (permission == null) {
            throw new BusinessException(4008, "权限不存在");
        }

        if (!Integer.valueOf(0).equals(request.getStatus()) && !Integer.valueOf(1).equals(request.getStatus())) {
            throw new BusinessException(4014, "权限状态值不合法");
        }

        if (!Integer.valueOf(1).equals(request.getPermType())
                && !Integer.valueOf(2).equals(request.getPermType())
                && !Integer.valueOf(3).equals(request.getPermType())) {
            throw new BusinessException(4019, "权限类型值不合法");
        }

        if ("admin:dashboard".equals(permission.getPermCode()) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BusinessException(4015, "核心权限不能被禁用");
        }

        permission.setPermName(request.getPermName());
        permission.setPermType(request.getPermType());
        permission.setPath(request.getPath());
        permission.setMethod(request.getMethod());
        permission.setStatus(request.getStatus());

        sysPermissionMapper.updateById(permission);
    }

    /**
     * 查询权限详情
     *
     * @param permissionId 权限ID
     * @return 权限详情
     */
    @Override
    public PermissionDetailResponse getPermissionDetail(Long permissionId) {
        SysPermission permission = sysPermissionMapper.selectById(permissionId);
        if (permission == null) {
            throw new BusinessException(4008, "权限不存在");
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