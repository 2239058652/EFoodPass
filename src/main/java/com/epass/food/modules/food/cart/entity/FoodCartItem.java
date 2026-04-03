package com.epass.food.modules.food.cart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("food_cart_item")
public class FoodCartItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long foodItemId;

    private Integer quantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
