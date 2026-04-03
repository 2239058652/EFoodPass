package com.epass.food.modules.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserSessionResponse {

    private String sessionId;

    private String requestIp;

    private String userAgent;

    private LocalDateTime loginTime;

    private LocalDateTime lastAccessTime;

    private LocalDateTime expiresAt;

    private Boolean current;
}
