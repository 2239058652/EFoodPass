package com.epass.food.modules.system.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类
 * 对应数据库表 sys_user，用于存储系统用户的基本信息
 */
@TableName("sys_user")
@Data
public class SysUser {
    /**
     * 用户主键 ID，自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名，用于登录认证
     */
    private String username;

    /**
     * 密码哈希值，加密存储
     */
    private String passwordHash;

    /**
     * 用户昵称，显示名称
     */
    private String nickname;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 用户状态，例如：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;

    /**
     * 记录创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * Token 版本号，用于控制会话有效性
     */
    private Integer tokenVersion;
}
