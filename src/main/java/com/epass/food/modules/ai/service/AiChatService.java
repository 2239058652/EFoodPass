package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiChatResponse;

public interface AiChatService {

    AiChatResponse chat(String message, Long currentUserId, boolean canViewAnyOrder);
}