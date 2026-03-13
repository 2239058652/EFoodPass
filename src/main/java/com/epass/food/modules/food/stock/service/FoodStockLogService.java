package com.epass.food.modules.food.stock.service;

import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.stock.dto.FoodStockLogListQuery;
import com.epass.food.modules.food.stock.dto.FoodStockLogListResponse;

public interface FoodStockLogService {

    void recordOrderDeduct(Long foodItemId, Integer beforeStock, Integer changeAmount, Long orderId);

    void recordOrderRestore(Long foodItemId, Integer beforeStock, Integer changeAmount, Long orderId);

    void recordManualAdjust(Long foodItemId, Integer beforeStock, Integer afterStock, String remark);

    PageResult<FoodStockLogListResponse> listLogs(FoodStockLogListQuery query);
}