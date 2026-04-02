package com.epass.food.modules.ai.dto;

public record AiSceneRequestContext(
        String message,
        String sessionId,
        Long currentUserId,
        boolean canViewAnyOrder
) {
}
