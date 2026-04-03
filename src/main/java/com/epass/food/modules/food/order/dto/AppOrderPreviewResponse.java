package com.epass.food.modules.food.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AppOrderPreviewResponse {

    private Integer totalQuantity;

    private BigDecimal totalAmount;

    private String remark;

    private List<AppOrderPreviewItemResponse> items;
}
