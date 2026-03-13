package com.epass.food.modules.auth.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.BizErrorCode;
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

    public AuthServiceImpl(SysUserService sysUserService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           SysRoleService sysRoleService,
                           SysPermissionService sysPermissionService) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysRoleService = sysRoleService;
        this.sysPermissionService = sysPermissionService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserService.getByUsername(request.getUsername());

        if (user == null) {
            throw new BusinessException(BizErrorCode.AUTH_USERNAME_OR_PASSWORD_INVALID, "用户名或密码错误");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(BizErrorCode.AUTH_USER_DISABLED, "用户已被禁用");
        }

        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!matched) {
            throw new BusinessException(BizErrorCode.AUTH_USERNAME_OR_PASSWORD_INVALID, "用户名或密码错误");
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername(), user.getTokenVersion());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "用户不存在");
        }

        List<SysRole> roleList = sysRoleService.getRolesByUserId(userId);
        List<String> permissionCodes = sysPermissionService.getPermissionCodesByUserId(userId);
        List<String> roleCodes = roleList.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setRoleCodes(roleCodes);
        response.setPermissionCodes(permissionCodes);
        return response;
    }
}
