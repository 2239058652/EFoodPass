package com.epass.food.modules.system.role.dto;

import lombok.Data;

@Data
public class RoleListResponse {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色状态：1启用，0禁用
     */
    private Integer status;
}