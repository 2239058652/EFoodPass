package com.epass.food.modules.system.user.dto;

import com.epass.food.common.page.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 当在类上使用 @EqualsAndHashCode(callSuper = true) 时，
 * Lombok 生成的 equals() 和 hashCode() 方法会显式调用父类的对应方法，
 * 并将其结果纳入当前类的计算逻辑中
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserListQuery extends PageQuery {

    /**
     * 用户名，支持模糊查询
     */
    private String username;

    /**
     * 用户状态：1启用，0禁用
     */
    private Integer status;
}