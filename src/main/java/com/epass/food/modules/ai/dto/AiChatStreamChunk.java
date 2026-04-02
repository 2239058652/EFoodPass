package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiChatStreamChunk {

    private String sessionId;

    private String scene;

    private String content;

    private boolean done;
}
