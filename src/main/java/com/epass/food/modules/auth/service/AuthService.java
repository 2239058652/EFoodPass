package com.epass.food.modules.auth.service;

import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;

public interface AuthService {

    // 登录
    LoginResponse login(LoginRequest request);
}
