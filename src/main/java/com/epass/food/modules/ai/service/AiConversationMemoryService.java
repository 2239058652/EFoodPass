package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiSceneType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AiConversationMemoryService {

    private static final int MAX_TURNS = 6;

    private final Map<String, Deque<ConversationTurn>> conversationMap = new ConcurrentHashMap<>();
    private final Map<String, AiSceneType> lastSceneMap = new ConcurrentHashMap<>();

    public String ensureSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId.trim();
        }
        return UUID.randomUUID().toString();
    }

    public List<ConversationTurn> getRecentTurns(Long userId, String sessionId, int limit) {
        Deque<ConversationTurn> deque = conversationMap.get(buildKey(userId, sessionId));
        if (deque == null || deque.isEmpty()) {
            return List.of();
        }

        List<ConversationTurn> turns = new ArrayList<>(deque);
        int fromIndex = Math.max(0, turns.size() - limit);
        return turns.subList(fromIndex, turns.size());
    }

    public Optional<AiSceneType> getLastScene(Long userId, String sessionId) {
        return Optional.ofNullable(lastSceneMap.get(buildKey(userId, sessionId)));
    }

    public void appendTurn(Long userId,
                           String sessionId,
                           AiSceneType sceneType,
                           String userMessage,
                           String assistantMessage) {
        String key = buildKey(userId, sessionId);
        Deque<ConversationTurn> deque = conversationMap.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(new ConversationTurn(userMessage, assistantMessage));
        while (deque.size() > MAX_TURNS) {
            deque.pollFirst();
        }
        lastSceneMap.put(key, sceneType);
    }

    private String buildKey(Long userId, String sessionId) {
        return userId + ":" + sessionId;
    }

    public record ConversationTurn(String userMessage, String assistantMessage) {
    }
}
