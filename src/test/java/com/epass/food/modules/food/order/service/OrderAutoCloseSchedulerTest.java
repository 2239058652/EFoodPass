package com.epass.food.modules.food.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderAutoCloseSchedulerTest {

    @Mock
    private FoodOrderService foodOrderService;

    private OrderAutoCloseProperties properties;
    private OrderAutoCloseScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new OrderAutoCloseProperties();
        scheduler = new OrderAutoCloseScheduler(foodOrderService, properties);
    }

    @Test
    void autoCloseShouldSkipWhenDisabled() {
        properties.setEnabled(false);

        scheduler.autoCloseExpiredOrders();

        verifyNoInteractions(foodOrderService);
    }

    @Test
    void autoCloseShouldInvokeServiceWithConfiguredValues() {
        properties.setEnabled(true);
        properties.setTimeoutMinutes(20);
        properties.setBatchSize(30);
        when(foodOrderService.closeExpiredUnpaidOrders(20, 30)).thenReturn(2);

        scheduler.autoCloseExpiredOrders();

        verify(foodOrderService).closeExpiredUnpaidOrders(20, 30);
    }
}
