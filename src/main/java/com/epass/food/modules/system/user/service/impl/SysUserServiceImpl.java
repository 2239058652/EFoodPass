package com.epass.food.modules.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.entity.SysUserRole;
import com.epass.food.modules.system.role.mapper.SysRoleMapper;
import com.epass.food.modules.system.role.mapper.SysUserRoleMapper;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.dto.UserAssignRoleRequest;
import com.epass.food.modules.system.user.dto.UserCreateRequest;
import com.epass.food.modules.system.user.dto.UserListQuery;
import com.epass.food.modules.system.user.dto.UserListResponse;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.mapper.SysUserMapper;
import com.epass.food.modules.system.user.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysRoleService sysRoleService;
    private final PasswordEncoder passwordEncoder;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    public SysUserServiceImpl(SysRoleService sysRoleService,
                              PasswordEncoder passwordEncoder,
                              SysUserRoleMapper sysUserRoleMapper,
                              SysRoleMapper sysRoleMapper) {
        this.sysRoleService = sysRoleService;
        this.passwordEncoder = passwordEncoder;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
    }

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

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @return 用户列表
     */
    @Override
    public List<UserListResponse> listUsers(UserListQuery query) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (query != null && StringUtils.hasText(query.getUsername())) {
            queryWrapper.like(SysUser::getUsername, query.getUsername());  // 用户名模糊查询
        }

        if (query != null && query.getStatus() != null) {
            queryWrapper.eq(SysUser::getStatus, query.getStatus()); // 按前端传的状态筛选
        }

        queryWrapper.orderByDesc(SysUser::getId); // 按 ID 倒序排列（最新数据在前面）

        List<SysUser> userList = this.list(queryWrapper);

        List<UserListResponse> responseList = new ArrayList<>();
        for (SysUser user : userList) {
            List<SysRole> roleList = sysRoleService.getRolesByUserId(user.getId());

            List<String> roleCodes = new ArrayList<>();
            for (SysRole role : roleList) {
                roleCodes.add(role.getRoleCode());
            }

            UserListResponse response = new UserListResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setNickname(user.getNickname());
            response.setPhone(user.getPhone());
            response.setStatus(user.getStatus());
            response.setRoleCodes(roleCodes);

            responseList.add(response);
        }

        return responseList;
    }

    /**
     * 创建用户
     *
     * @param request 创建用户的请求参数
     */
    @Override
    public void createUser(UserCreateRequest request) {
        SysUser existUser = this.getByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(4001, "用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus());
        user.setTokenVersion(0);

        this.save(user);
    }

    /**
     * 分配角色
     *
     * @param request 分配角色的请求参数
     */
    @Override
    public void assignRoles(UserAssignRoleRequest request) {
        SysUser user = this.getById(request.getUserId());
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        Long roleCount = sysRoleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, request.getRoleIds())
                        .eq(SysRole::getStatus, 1)
        );
        if (roleCount == null || roleCount != request.getRoleIds().size()) {
            throw new BusinessException(4005, "角色不存在或已禁用");
        }

        sysUserRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, request.getUserId())
        );

        for (Long roleId : request.getRoleIds()) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(request.getUserId());
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }
}
