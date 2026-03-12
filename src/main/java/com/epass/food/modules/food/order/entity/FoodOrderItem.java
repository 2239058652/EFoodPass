package com.epass.food.modules.food.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("food_order_item")
public class FoodOrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long foodItemId;

    private String foodNameSnapshot;

    private BigDecimal priceSnapshot;

    private Integer quantity;

    private BigDecimal amount;

    private LocalDateTime createdAt;
}