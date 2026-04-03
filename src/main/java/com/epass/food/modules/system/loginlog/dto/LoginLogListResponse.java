package com.epass.food.modules.system.loginlog.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoginLogListResponse {

    private Long id;

    private Long userId;

    private String username;

    private String requestIp;

    private Integer success;

    private String message;

    private LocalDateTime loginTime;
}
