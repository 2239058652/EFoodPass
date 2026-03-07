package com.epass.food.modules.system.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.system.permission.dto.PermissionCreateRequest;
import com.epass.food.modules.system.permission.dto.PermissionListQuery;
import com.epass.food.modules.system.permission.dto.PermissionListResponse;
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
}