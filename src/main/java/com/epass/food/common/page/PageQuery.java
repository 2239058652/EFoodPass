package com.epass.food.common.page;

import lombok.Data;

@Data
public class PageQuery {

    /**
     * 页码，从1开始
     */
    private Long pageNum = 1L;

    /**
     * 每页大小
     */
    private Long pageSize = 10L;
}