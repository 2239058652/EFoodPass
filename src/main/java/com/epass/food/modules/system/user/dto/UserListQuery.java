package com.epass.food.modules.system.user.dto;

import lombok.Data;

@Data
public class UserListQuery {

    /**
     * 用户名，支持模糊查询
     */
    private String username;

    /**
     * 用户状态：1启用，0禁用
     */
    private Integer status;
}