package com.epass.food.modules.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.entity.SysUserRole;
import com.epass.food.modules.system.role.mapper.SysRoleMapper;
import com.epass.food.modules.system.role.mapper.SysUserRoleMapper;
import com.epass.food.modules.system.role.service.SysRoleService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;

    public SysRoleServiceImpl(SysUserRoleMapper sysUserRoleMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
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
}
