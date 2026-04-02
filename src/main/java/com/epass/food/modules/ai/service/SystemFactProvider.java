package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class SystemFactProvider {

    private final SystemModuleCatalog systemModuleCatalog;

    public SystemFactProvider(SystemModuleCatalog systemModuleCatalog) {
        this.systemModuleCatalog = systemModuleCatalog;
    }

    public String buildSystemFacts() {
        String moduleFacts = systemModuleCatalog.getModules().stream()
                .map(module -> "- %s：%s".formatted(module.code(), module.description()))
                .collect(java.util.stream.Collectors.joining("\n"));

        return """
                当前系统管理领域的真实业务事实：
                1. auth 模块负责登录和获取当前用户信息
                2. system 模块包括用户、角色、权限管理
                3. 系统采用 JWT + Spring Security 做认证鉴权
                4. 权限控制主要通过 @PreAuthorize 实现
                5. 当前模块清单如下：
                %s
                """.formatted(moduleFacts);
    }

}