package com.epass.food.common.page;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long pageNum;

    /**
     * 每页大小
     */
    private Long pageSize;

    /**
     * 当前页数据
     */
    private List<T> records;
}