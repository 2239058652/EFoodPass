package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiConversationMessage {

    private String id;

    private String role;

    private String content;

    private Long createdAt;
}
