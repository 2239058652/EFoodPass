package com.epass.food.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CurrentUserChangePasswordRequest {

    @NotBlank(message = "oldPassword must not be blank")
    private String oldPassword;

    @NotBlank(message = "newPassword must not be blank")
    private String newPassword;
}
