package com.epass.food.modules.system.permission.dto;

import lombok.Data;

@Data
public class PermissionListQuery {

    /**
     * 权限编码，支持模糊查询
     */
    private String permCode;

    /**
     * 权限状态：1启用，0禁用
     */
    private Integer status;
}