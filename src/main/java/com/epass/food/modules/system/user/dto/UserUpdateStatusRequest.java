package com.epass.food.modules.system.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateStatusRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户状态：1启用，0禁用
     */
    @NotNull(message = "用户状态不能为空")
    private Integer status;
}