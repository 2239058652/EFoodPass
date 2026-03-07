package com.epass.food.modules.system.role.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.modules.system.role.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    // 根据用户id查询角色列表
    List<SysRole> getRolesByUserId(Long userId);
}
