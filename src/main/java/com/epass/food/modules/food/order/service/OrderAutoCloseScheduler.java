package com.epass.food.modules.food.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderAutoCloseScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCloseScheduler.class);

    private final FoodOrderService foodOrderService;
    private final OrderAutoCloseProperties properties;

    public OrderAutoCloseScheduler(FoodOrderService foodOrderService,
                                   OrderAutoCloseProperties properties) {
        this.foodOrderService = foodOrderService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${order.auto-close.fixed-delay-ms:60000}")
    public void autoCloseExpiredOrders() {
        if (!properties.isEnabled()) {
            return;
        }

        int closedCount = foodOrderService.closeExpiredUnpaidOrders(
                properties.getTimeoutMinutes(),
                properties.getBatchSize()
        );

        if (closedCount > 0) {
            log.info("Auto-closed {} expired unpaid orders", closedCount);
        }
    }
}
