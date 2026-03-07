package com.epass.food.modules.auth.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.service.AuthService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.service.SysUserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;

    public AuthServiceImpl(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
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

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean matched = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!matched) {
            throw new BusinessException(4001, "用户名或密码错误");
        }
        return new LoginResponse(user.getId(), user.getUsername(), user.getNickname());

    }
}
