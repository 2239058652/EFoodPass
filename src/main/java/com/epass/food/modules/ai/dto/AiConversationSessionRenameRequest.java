package com.epass.food.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiConversationSessionRenameRequest {

    @NotBlank(message = "title 不能为空")
    private String title;
}
