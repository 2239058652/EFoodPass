package com.epass.food.modules.system.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDetailResponse {

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
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 已分配的角色ID列表
     */
    private List<Long> roleIds;
}