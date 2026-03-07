package com.epass.food.modules.system.role.dto;

import lombok.Data;

@Data
public class RoleListQuery {

    /**
     * 角色编码，支持模糊查询
     */
    private String roleCode;

    /**
     * 角色状态：1启用，0禁用
     */
    private Integer status;
}