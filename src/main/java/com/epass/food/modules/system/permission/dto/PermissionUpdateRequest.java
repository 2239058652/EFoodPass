package com.epass.food.modules.system.permission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionUpdateRequest {

    /**
     * 权限ID
     */
    @NotNull(message = "权限ID不能为空")
    private Long id;

    /**
     * 权限名称
     */
    @NotBlank(message = "权限名称不能为空")
    private String permName;

    /**
     * 权限类型：1目录 2菜单 3接口
     */
    @NotNull(message = "权限类型不能为空")
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
    @NotNull(message = "权限状态不能为空")
    private Integer status;
}