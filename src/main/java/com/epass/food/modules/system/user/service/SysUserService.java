package com.epass.food.modules.system.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.modules.system.user.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    // 根据用户名查询用户
    SysUser getByUsername(String username);

    // 根据用户ID查询角色列表
}
