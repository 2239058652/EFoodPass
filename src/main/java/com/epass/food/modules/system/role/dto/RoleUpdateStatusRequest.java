package com.epass.food.modules.system.role.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleUpdateStatusRequest {

    /**
     * 角色ID
     */
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /**
     * 角色状态：1启用，0禁用
     */
    @NotNull(message = "角色状态不能为空")
    private Integer status;
}