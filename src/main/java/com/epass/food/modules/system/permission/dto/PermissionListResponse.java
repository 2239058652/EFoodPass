package com.epass.food.modules.system.permission.dto;

import lombok.Data;

@Data
public class PermissionListResponse {

    /**
     * 权限ID
     */
    private Long id;

    /**
     * 权限编码
     */
    private String permCode;

    /**
     * 权限名称
     */
    private String permName;

    /**
     * 权限类型：1目录 2菜单 3接口
     */
    private Integer permType;

    /**
     * 请求路径
     */
    private String path;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 权限状态：1启用，0禁用
     */
    private Integer status;
}