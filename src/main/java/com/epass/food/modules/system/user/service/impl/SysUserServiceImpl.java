package com.epass.food.modules.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.mapper.SysUserMapper;
import com.epass.food.modules.system.user.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 根据用户名查询系统用户信息
     *
     * @param username 用户名，用于查询的唯一标识
     * @return SysUser 匹配的用户对象，如果未找到则返回 null
     * SELECT * FROM sys_user WHERE username = '你传入的用户名' LIMIT 1;
     */
    @Override
    public SysUser getByUsername(String username) {
        // 1. 创建一个针对 SysUser 实体类的 Lambda 条件构造器
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        // 2. 拼接 SQL 里的 WHERE 条件
        // 相当于生成 SQL: WHERE username = 传入的参数值
        queryWrapper.eq(SysUser::getUsername, username);

        // 3. 在生成的 SQL 语句最后面强行加上 "limit 1"
        // 相当于生成 SQL: WHERE username = ? limit 1
        queryWrapper.last("limit 1");

        // 4. 执行查询，并返回查询到的单个对象
        return this.getOne(queryWrapper);
    }
}
