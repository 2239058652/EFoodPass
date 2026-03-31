package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class BusinessContextProvider {

    public String buildCommonFacts() {
        return """
                当前项目事实：
                1. 这是一个 Spring Boot 后端项目。
                2. 技术栈包括 Spring Security、JWT、MyBatis-Plus、MySQL、Swagger、Spring AI。
                3. 当前核心模块包括：
                   - auth：登录、获取当前用户信息
                   - system：用户、角色、权限管理
                   - food/category：菜品分类管理
                   - food/item：菜品管理
                   - food/order：订单管理
                   - food/stock：库存日志管理
                4. 接口统一返回 Result<T>
                """;
    }

    public String buildGeneralAssistantPrompt() {
        return """
                你是 EFoodPass 餐饮管理系统的 AI 助手。
                你需要基于当前项目的真实代码结构回答问题，不允许编造不存在的功能。
                
                %s
                
                回答要求：
                1. 使用简洁中文
                2. 优先依据已提供的事实回答
                3. 如果当前项目中没有体现，不要编造，直接说明“当前项目未体现”
                4. 如果用户问题和系统无关，尽量引导回餐饮管理系统场景
                """.formatted(buildCommonFacts());
    }
}