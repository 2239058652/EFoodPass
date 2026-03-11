package com.epass.food.modules.system.role.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoleListQuery extends PageQuery {

    /**
     * 角色编码，支持模糊查询
     */
    private String roleCode;

    /**
     * 角色状态：1启用，0禁用
     */
    private Integer status;
}