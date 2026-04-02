package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemModuleCatalog {

    public List<ModuleInfo> getModules() {
        return List.of(
                new ModuleInfo("auth", "认证模块", "登录、获取当前用户信息"),
                new ModuleInfo("system", "系统管理", "用户、角色、权限管理"),
                new ModuleInfo("food/category", "菜品分类", "菜品分类管理"),
                new ModuleInfo("food/item", "菜品管理", "菜品新增、修改、上下架、库存调整"),
                new ModuleInfo("food/order", "订单管理", "订单查询、处理、统计"),
                new ModuleInfo("food/stock", "库存日志", "库存变更日志查询")
        );
    }

    public record ModuleInfo(String code, String name, String description) {
    }
}
