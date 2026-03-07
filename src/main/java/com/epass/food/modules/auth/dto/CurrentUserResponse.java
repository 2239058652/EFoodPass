package com.epass.food.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserResponse {

    private Long userId;
    private String username;
    private String nickname;

}
