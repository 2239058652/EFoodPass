package com.epass.food.modules.food.stock.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("food_stock_log")
public class FoodStockLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long foodItemId;

    /**
     * 1下单扣减 2取消回补 3后台调整
     */
    private Integer changeType;

    /**
     * 扣减为负数，回补为正数
     */
    private Integer changeAmount;

    private Integer beforeStock;

    private Integer afterStock;

    private Long bizId;

    private String remark;

    private LocalDateTime createdAt;
}