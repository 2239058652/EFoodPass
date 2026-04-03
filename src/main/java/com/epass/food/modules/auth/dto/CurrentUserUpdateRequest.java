package com.epass.food.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CurrentUserUpdateRequest {

    @NotBlank(message = "nickname must not be blank")
    private String nickname;

    private String phone;
}
