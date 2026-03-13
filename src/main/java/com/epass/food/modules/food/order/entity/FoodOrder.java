package com.epass.food.modules.food.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("food_order")
public class FoodOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private BigDecimal totalAmount;

    /**
     * 10待确认 20制作中 30已完成 40已取消
     */
    private Integer orderStatus;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}