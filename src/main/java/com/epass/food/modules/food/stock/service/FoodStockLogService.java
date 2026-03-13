package com.epass.food.modules.food.stock.service;

public interface FoodStockLogService {

    void recordOrderDeduct(Long foodItemId, Integer beforeStock, Integer changeAmount, Long orderId);

    void recordOrderRestore(Long foodItemId, Integer beforeStock, Integer changeAmount, Long orderId);

    void recordManualAdjust(Long foodItemId, Integer beforeStock, Integer afterStock, String remark);
}