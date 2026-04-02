package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.dto.AiStructuredReply;
import com.epass.food.modules.ai.service.AiChatService;
import com.epass.food.modules.ai.service.AiConversationMemoryService;
import com.epass.food.modules.ai.service.AiSceneClassifier;
import com.epass.food.modules.ai.service.AiSceneHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final AiSceneClassifier aiSceneClassifier;
    private final ObjectMapper objectMapper;
    private final AiConversationMemoryService conversationMemoryService;
    private final Map<AiSceneType, AiSceneHandler> sceneHandlerMap;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             AiSceneClassifier aiSceneClassifier,
                             ObjectMapper objectMapper,
                             AiConversationMemoryService conversationMemoryService,
                             List<AiSceneHandler> sceneHandlers) {
        this.chatClient = chatClientBuilder.build();
        this.aiSceneClassifier = aiSceneClassifier;
        this.objectMapper = objectMapper;
        this.conversationMemoryService = conversationMemoryService;
        this.sceneHandlerMap = buildSceneHandlerMap(sceneHandlers);
    }

    @Override
    public AiChatResponse chat(String message, String sessionId, Long currentUserId, boolean canViewAnyOrder) {
        String resolvedSessionId = conversationMemoryService.ensureSessionId(sessionId);
        AiSceneType sceneType = resolveSceneType(message, currentUserId, resolvedSessionId);
        AiSceneRequestContext context = new AiSceneRequestContext(
                message,
                resolvedSessionId,
                currentUserId,
                canViewAnyOrder
        );
        AiPromptPlan promptPlan = buildPromptByScene(sceneType, context);
        String promptWithHistory = appendConversationHistory(promptPlan.prompt(), currentUserId, resolvedSessionId);

        String rawContent = chatClient.prompt()
                .system(promptWithHistory)
                .user(message)
                .call()
                .content();

        AiStructuredReply reply = parseStructuredReply(rawContent);
        conversationMemoryService.appendTurn(currentUserId, resolvedSessionId, sceneType, message, reply.getContent());
        return new AiChatResponse(
                resolvedSessionId,
                reply.getContent(),
                sceneType.name().toLowerCase(),
                promptPlan.grounded(),
                promptPlan.nextAction(),
                promptPlan.answerType().name().toLowerCase(),
                promptPlan.card()
        );
    }

    @Override
    public void clearSession(String sessionId, Long currentUserId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(400, "sessionId 不能为空");
        }
        conversationMemoryService.clearSession(currentUserId, sessionId.trim());
    }

    private AiPromptPlan buildPromptByScene(AiSceneType sceneType, AiSceneRequestContext context) {
        AiSceneHandler sceneHandler = sceneHandlerMap.get(sceneType);
        if (sceneHandler == null) {
            throw new BusinessException(500, "未找到 AI 场景处理器: " + sceneType);
        }
        return sceneHandler.buildPlan(context);
    }

    private AiSceneType resolveSceneType(String message, Long currentUserId, String sessionId) {
        AiSceneType sceneType = aiSceneClassifier.classify(message);
        if (sceneType != AiSceneType.GENERAL) {
            return sceneType;
        }

        if (!shouldReuseLastScene(message)) {
            return sceneType;
        }

        return conversationMemoryService.getLastScene(currentUserId, sessionId).orElse(AiSceneType.GENERAL);
    }

    private boolean shouldReuseLastScene(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String trimmed = message.trim();
        return trimmed.length() <= 12
                || trimmed.contains("那")
                || trimmed.contains("这个")
                || trimmed.contains("它")
                || trimmed.contains("继续")
                || trimmed.contains("然后")
                || trimmed.contains("还有");
    }

    private String appendConversationHistory(String basePrompt, Long currentUserId, String sessionId) {
        AiConversationMemoryService.ConversationPromptContext promptContext =
                conversationMemoryService.getPromptContext(currentUserId, sessionId);

        boolean hasSummary = StringUtils.hasText(promptContext.summary());
        boolean hasRecentTurns = !promptContext.recentTurns().isEmpty();
        if (!hasSummary && !hasRecentTurns) {
            return basePrompt;
        }

        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("\n\n下面是与当前会话相关的上下文，仅用于辅助理解当前问题：\n");
        if (hasSummary) {
            historyBuilder.append(promptContext.summary()).append("\n");
        }

        if (hasRecentTurns) {
            historyBuilder.append("最近几轮对话：\n");
        }

        int index = 1;
        for (AiConversationMemoryService.ConversationTurn turn : promptContext.recentTurns()) {
            historyBuilder.append(index)
                    .append(". 用户：")
                    .append(turn.userMessage())
                    .append("\n");
            historyBuilder.append(index)
                    .append(". 助手：")
                    .append(turn.assistantMessage())
                    .append("\n");
            index++;
        }

        historyBuilder.append("回答当前问题时优先依据本轮问题；如果历史上下文与本轮冲突，以本轮为准。");
        return basePrompt + historyBuilder;
    }

    private AiStructuredReply parseStructuredReply(String rawContent) {
        try {
            return objectMapper.readValue(rawContent, AiStructuredReply.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "AI 返回结果不是合法 JSON: " + rawContent);
        }
    }

    private Map<AiSceneType, AiSceneHandler> buildSceneHandlerMap(List<AiSceneHandler> sceneHandlers) {
        Map<AiSceneType, AiSceneHandler> handlerMap = new EnumMap<>(AiSceneType.class);
        for (AiSceneHandler sceneHandler : sceneHandlers) {
            handlerMap.put(sceneHandler.sceneType(), sceneHandler);
        }
        return handlerMap;
    }
}
