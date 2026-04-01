package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class SystemFactProvider {

    public String buildSystemFacts() {
        return """
                当前系统管理领域的真实业务事实：
                1. auth 模块负责登录和获取当前用户信息
                2. system 模块包括用户、角色、权限管理
                3. 系统采用 JWT + Spring Security 做认证鉴权
                4. 权限控制主要通过 @PreAuthorize 实现
                """;
    }
}