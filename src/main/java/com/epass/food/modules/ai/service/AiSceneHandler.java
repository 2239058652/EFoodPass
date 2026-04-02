package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneType;

public interface AiSceneHandler {

    AiSceneType sceneType();

    AiPromptPlan buildPlan(String message, Long currentUserId, boolean canViewAnyOrder);
}
