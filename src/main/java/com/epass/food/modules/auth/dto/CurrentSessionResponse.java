package com.epass.food.modules.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CurrentSessionResponse {

    private Long userId;

    private String sessionId;

    private String username;

    private String nickname;

    private Integer tokenVersion;

    private String requestIp;

    private String userAgent;

    private LocalDateTime loginTime;

    private LocalDateTime lastAccessTime;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private Long remainingSeconds;
}
