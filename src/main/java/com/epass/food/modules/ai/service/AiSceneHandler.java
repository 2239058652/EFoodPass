package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;

public interface AiSceneHandler {

    AiSceneType sceneType();

    AiPromptPlan buildPlan(AiSceneRequestContext context);
}
