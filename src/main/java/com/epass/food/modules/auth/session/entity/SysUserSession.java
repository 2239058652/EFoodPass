package com.epass.food.modules.auth.session.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user_session")
public class SysUserSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sessionId;

    private Integer tokenVersion;

    private String requestIp;

    private String userAgent;

    private LocalDateTime loginTime;

    private LocalDateTime lastAccessTime;

    private LocalDateTime expireTime;
}
