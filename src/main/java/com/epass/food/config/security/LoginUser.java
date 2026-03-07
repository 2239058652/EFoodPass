package com.epass.food.config.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户
 * 它不是数据库实体，也不是前端返回对象
 * 而是给 Spring Security 在认证上下文里提供信息的
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUser {

    private Long userId;
    private String username;
    private String nickname;
}