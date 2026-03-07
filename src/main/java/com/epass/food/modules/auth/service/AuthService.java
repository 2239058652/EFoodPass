package com.epass.food.modules.auth.service;

import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;

public interface AuthService {

    // 登录
    LoginResponse login(LoginRequest request);

    // 获取当前用户信息
    CurrentUserResponse getCurrentUser(Long userId);
}
