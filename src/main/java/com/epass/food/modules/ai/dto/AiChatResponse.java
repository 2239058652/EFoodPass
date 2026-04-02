package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiChatResponse {

    private String sessionId;

    private String content;

    private String scene;

    private Boolean grounded;

    private String nextAction;

    private String answerType;

    private AiDisplayCard card;
}
