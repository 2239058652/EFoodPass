package com.epass.food.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request")
public class LoginRequest {

    @Schema(description = "Username", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "username must not be blank")
    private String username;

    @Schema(description = "Password", example = "Admin@123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "password must not be blank")
    private String password;
}
