package com.epass.food.modules.system.operationlog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogListResponse {

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
