package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiConversationSessionDetail;
import com.epass.food.modules.ai.dto.AiConversationSessionSummary;

import java.util.List;

public interface AiChatService {

    AiChatResponse chat(String message, String sessionId, Long currentUserId, boolean canViewAnyOrder);

    void clearSession(String sessionId, Long currentUserId);

    List<AiConversationSessionSummary> listSessions(Long currentUserId, int limit);

    AiConversationSessionDetail getSessionDetail(String sessionId, Long currentUserId, int pageNum, int pageSize);

    void renameSession(String sessionId, String title, Long currentUserId);
}
