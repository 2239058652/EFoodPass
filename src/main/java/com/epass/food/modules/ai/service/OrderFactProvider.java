package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

@Component
public class OrderFactProvider {

    public String buildOrderStatusFacts() {
        return """
                当前系统中的真实订单状态如下：
                - 10：待确认
                - 20：制作中
                - 30：已完成
                - 40：已取消
                """;
    }

    public String buildOrderRulesFacts() {
        return """
                当前系统中的真实订单规则如下：
                1. 后台订单接口是 /food/order/**
                2. 用户端订单接口是 /app/order/**
                3. 下单时会校验用户、菜品、分类、库存
                4. 下单会扣减库存
                5. 取消订单会回补库存
                """;
    }

    public String buildOrderFacts() {
        return buildOrderStatusFacts() + "\n" + buildOrderRulesFacts();
    }
}