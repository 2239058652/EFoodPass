package com.epass.food.modules.system.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserListResponse {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 用户状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 角色编码列表
     */
    private List<String> roleCodes;
}