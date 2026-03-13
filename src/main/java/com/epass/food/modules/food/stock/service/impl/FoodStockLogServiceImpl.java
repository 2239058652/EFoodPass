package com.epass.food.modules.food.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.food.item.entity.FoodItem;
import com.epass.food.modules.food.item.mapper.FoodItemMapper;
import com.epass.food.modules.food.stock.dto.FoodStockLogListQuery;
import com.epass.food.modules.food.stock.dto.FoodStockLogListResponse;
import com.epass.food.modules.food.stock.entity.FoodStockLog;
import com.epass.food.modules.food.stock.mapper.FoodStockLogMapper;
import com.epass.food.modules.food.stock.service.FoodStockLogService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FoodStockLogServiceImpl implements FoodStockLogService {

    private static final int CHANGE_TYPE_ORDER_DEDUCT = 1;
    private static final int CHANGE_TYPE_ORDER_RESTORE = 2;
    private static final int CHANGE_TYPE_MANUAL_ADJUST = 3;

    private final FoodStockLogMapper foodStockLogMapper;
    private final FoodItemMapper foodItemMapper;

    public FoodStockLogServiceImpl(FoodStockLogMapper foodStockLogMapper,
                                   FoodItemMapper foodItemMapper) {
        this.foodStockLogMapper = foodStockLogMapper;
        this.foodItemMapper = foodItemMapper;
    }

    private void validateChangeType(Integer changeType) {
        if (!Integer.valueOf(CHANGE_TYPE_ORDER_DEDUCT).equals(changeType)
                && !Integer.valueOf(CHANGE_TYPE_ORDER_RESTORE).equals(changeType)
                && !Integer.valueOf(CHANGE_TYPE_MANUAL_ADJUST).equals(changeType)) {
            throw new BusinessException(4401, "库存变动类型不合法");
        }
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

    @Override
    public PageResult<FoodStockLogListResponse> listLogs(FoodStockLogListQuery query) {
        if (query == null) {
            query = new FoodStockLogListQuery();
        }

        LambdaQueryWrapper<FoodStockLog> queryWrapper = new LambdaQueryWrapper<>();

        if (query.getFoodItemId() != null) {
            queryWrapper.eq(FoodStockLog::getFoodItemId, query.getFoodItemId());
        }

        if (query.getChangeType() != null) {
            validateChangeType(query.getChangeType());
            queryWrapper.eq(FoodStockLog::getChangeType, query.getChangeType());
        }

        queryWrapper.orderByDesc(FoodStockLog::getId);

        Page<FoodStockLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<FoodStockLog> logPage = foodStockLogMapper.selectPage(page, queryWrapper);

        List<FoodStockLogListResponse> responseList = new ArrayList<>();
        for (FoodStockLog log : logPage.getRecords()) {
            FoodItem item = foodItemMapper.selectById(log.getFoodItemId());

            FoodStockLogListResponse response = new FoodStockLogListResponse();
            response.setId(log.getId());
            response.setFoodItemId(log.getFoodItemId());
            response.setFoodItemName(item == null ? null : item.getName());
            response.setChangeType(log.getChangeType());
            response.setChangeAmount(log.getChangeAmount());
            response.setBeforeStock(log.getBeforeStock());
            response.setAfterStock(log.getAfterStock());
            response.setBizId(log.getBizId());
            response.setRemark(log.getRemark());
            response.setCreatedAt(log.getCreatedAt());
            responseList.add(response);
        }

        PageResult<FoodStockLogListResponse> result = new PageResult<>();
        result.setTotal(logPage.getTotal());
        result.setPageNum(logPage.getCurrent());
        result.setPageSize(logPage.getSize());
        result.setRecords(responseList);
        return result;
    }
}