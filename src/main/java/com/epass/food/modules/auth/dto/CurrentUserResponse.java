package com.epass.food.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String phone;
    private LocalDateTime lastLoginAt;
    private List<String> roleCodes;
    private List<String> permissionCodes;
}
