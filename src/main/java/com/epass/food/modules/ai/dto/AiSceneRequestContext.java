package com.epass.food.modules.ai.dto;

public record AiSceneRequestContext(
        String message,
        Long currentUserId,
        boolean canViewAnyOrder
) {
}
