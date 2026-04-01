package com.epass.food.modules.ai.service;

import com.epass.food.modules.food.order.enums.FoodOrderStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class OrderFactProvider {

    public String buildOrderStatusFacts() {
        String statusLines = Arrays.stream(FoodOrderStatus.values())
                .map(status -> "- %d：%s".formatted(status.getCode(), status.getLabel()))
                .collect(Collectors.joining("\n"));

        return """
                当前系统中的真实订单状态如下：
                %s
                """.formatted(statusLines);
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