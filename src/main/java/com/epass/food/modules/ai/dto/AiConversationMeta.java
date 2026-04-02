package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiConversationMeta {

    private Boolean historyApplied;

    private Boolean summaryApplied;

    private Integer recentTurnCount;

    private Boolean sceneReused;
}
