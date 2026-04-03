package com.epass.food.modules.system.operationlog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestId;

    private Long userId;

    private String username;

    private String module;

    private String action;

    private String method;

    private String path;

    private String requestIp;

    private Integer success;

    private String errorMessage;

    private Integer costMs;

    private LocalDateTime operateTime;
}
