package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AiConversationSessionDetail {

    private String sessionId;

    private String title;

    private String scene;

    private String preview;

    private Long updatedAt;

    private String summary;

    private List<AiConversationMessage> messages;
}
