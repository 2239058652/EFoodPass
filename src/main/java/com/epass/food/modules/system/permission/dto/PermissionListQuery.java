package com.epass.food.modules.system.permission.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionListQuery extends PageQuery {

    /**
     * 权限编码，支持模糊查询
     */
    private String permCode;

    /**
     * 权限状态：1启用，0禁用
     */
    private Integer status;
}