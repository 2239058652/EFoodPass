package com.epass.food.modules.food.stock.service.impl;

import com.epass.food.modules.food.stock.entity.FoodStockLog;
import com.epass.food.modules.food.stock.mapper.FoodStockLogMapper;
import com.epass.food.modules.food.stock.service.FoodStockLogService;
import org.springframework.stereotype.Service;

@Service
public class FoodStockLogServiceImpl implements FoodStockLogService {

    private static final int CHANGE_TYPE_ORDER_DEDUCT = 1;
    private static final int CHANGE_TYPE_ORDER_RESTORE = 2;
    private static final int CHANGE_TYPE_MANUAL_ADJUST = 3;

    private final FoodStockLogMapper foodStockLogMapper;

    public FoodStockLogServiceImpl(FoodStockLogMapper foodStockLogMapper) {
        this.foodStockLogMapper = foodStockLogMapper;
    }

    @Override
    public void recordOrderDeduct(Long foodItemId, Integer beforeStock, Integer changeAmount, Long orderId) {
        FoodStockLog log = new FoodStockLog();
        log.setFoodItemId(foodItemId);
        log.setChangeType(CHANGE_TYPE_ORDER_DEDUCT);
        log.setChangeAmount(-changeAmount);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(beforeStock - changeAmount);
        log.setBizId(orderId);
        log.setRemark("订单创建扣减库存");
        foodStockLogMapper.insert(log);
    }

    @Override
    public void recordOrderRestore(Long foodItemId, Integer beforeStock, Integer changeAmount, Long orderId) {
        FoodStockLog log = new FoodStockLog();
        log.setFoodItemId(foodItemId);
        log.setChangeType(CHANGE_TYPE_ORDER_RESTORE);
        log.setChangeAmount(changeAmount);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(beforeStock + changeAmount);
        log.setBizId(orderId);
        log.setRemark("订单取消回补库存");
        foodStockLogMapper.insert(log);
    }

    @Override
    public void recordManualAdjust(Long foodItemId, Integer beforeStock, Integer afterStock, String remark) {
        FoodStockLog log = new FoodStockLog();
        log.setFoodItemId(foodItemId);
        log.setChangeType(CHANGE_TYPE_MANUAL_ADJUST);
        log.setChangeAmount(afterStock - beforeStock);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setBizId(null);
        log.setRemark(remark);
        foodStockLogMapper.insert(log);
    }
}