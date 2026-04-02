package com.epass.food.modules.ai.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StockChangeSceneCatalog {

    public List<ChangeScene> getScenes() {
        return List.of(
                new ChangeScene("order-deduct", "下单扣减"),
                new ChangeScene("order-restore", "取消回补"),
                new ChangeScene("manual-adjust", "后台手动调整")
        );
    }

    public record ChangeScene(String code, String label) {
    }
}