package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiConversationSessionSummary {

    private String sessionId;

    private String title;

    private String scene;

    private String preview;

    private Long updatedAt;
}
