package com.epass.food.modules.system.permission.service;

import java.util.List;

public interface SysPermissionService {

    // 根据用户ID查询权限列表 返回这个用户拥有的所有权限编码
    List<String> getPermissionCodesByUserId(Long userId);
}