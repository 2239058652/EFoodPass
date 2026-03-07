package com.epass.food.modules.auth.controller;

import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 登录 接口
     */
    @RequestMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success(response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<CurrentUserResponse> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        CurrentUserResponse response = authService.getCurrentUser(loginUser.getUserId());
        return Result.success(response);
    }
}
