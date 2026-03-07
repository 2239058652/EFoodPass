package com.epass.food.modules.system.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.modules.system.user.entity.SysUser;

public interface SysUserService extends IService<SysUser> {

    SysUser getByUsername(String username);
}
