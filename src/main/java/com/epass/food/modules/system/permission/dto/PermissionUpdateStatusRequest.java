package com.epass.food.modules.system.permission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PermissionUpdateStatusRequest {

    /**
     * 权限ID
     */
    @NotNull(message = "权限ID不能为空")
    private Long permissionId;

    /**
     * 权限状态：1启用，0禁用
     */
    @NotNull(message = "权限状态不能为空")
    private Integer status;
}