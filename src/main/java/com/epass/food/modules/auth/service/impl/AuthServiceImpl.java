package com.epass.food.modules.auth.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.config.security.JwtTokenProvider;
import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.service.AuthService;
import com.epass.food.modules.system.permission.service.SysPermissionService;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.service.SysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;

    public AuthServiceImpl(SysUserService sysUserService, PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider, SysRoleService sysRoleService,
                           SysPermissionService sysPermissionService) {

        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysRoleService = sysRoleService;
        this.sysPermissionService = sysPermissionService;
    }

    /**
     * 登录
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserService.getByUsername(request.getUsername());

        if (user == null) {
            throw new BusinessException(4001, "用户名或密码错误");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(4002, "用户已被禁用");
        }

        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!matched) {
            throw new BusinessException(4001, "用户名或密码错误");
        }

        // 生成 JWT token 返回登录响应结果带上 token
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getTokenVersion());

        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }

    /**
     * 获取当前用户信息
     *
     * @param userId 用户ID
     * @return 当前用户信息
     */
    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {

        // 根据用户ID查询用户信息
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(4004, "用户不存在");
        }

        // 根据用户ID查询用户角色列表
        List<SysRole> roleList = sysRoleService.getRolesByUserId(userId);

        // 根据用户ID查询用户权限列表
        List<String> permissionCodes = sysPermissionService.getPermissionCodesByUserId(userId);

        // 把角色对象列表转换成角色编码列表
        List<String> roleCodes = roleList.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        // 封装当前用户信息 返回
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoleCodes(roleCodes);
        response.setPermissionCodes(permissionCodes);

        return response;
    }
}
