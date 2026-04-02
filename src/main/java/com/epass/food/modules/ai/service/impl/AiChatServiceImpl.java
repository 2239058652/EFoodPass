package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.dto.AiStructuredReply;
import com.epass.food.modules.ai.service.AiChatService;
import com.epass.food.modules.ai.service.AiSceneHandler;
import com.epass.food.modules.ai.service.AiSceneClassifier;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final AiSceneClassifier aiSceneClassifier;
    private final ObjectMapper objectMapper;
    private final Map<AiSceneType, AiSceneHandler> sceneHandlerMap;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             AiSceneClassifier aiSceneClassifier,
                             ObjectMapper objectMapper,
                             List<AiSceneHandler> sceneHandlers) {
        this.chatClient = chatClientBuilder.build();
        this.aiSceneClassifier = aiSceneClassifier;
        this.objectMapper = objectMapper;
        this.sceneHandlerMap = buildSceneHandlerMap(sceneHandlers);
    }

    @Override
    public AiChatResponse chat(String message, Long currentUserId, boolean canViewAnyOrder) {
        AiSceneRequestContext context = new AiSceneRequestContext(message, currentUserId, canViewAnyOrder);
        AiSceneType sceneType = aiSceneClassifier.classify(message);
        AiPromptPlan promptPlan = buildPromptByScene(sceneType, context);

        String rawContent = chatClient.prompt()
                .system(promptPlan.prompt())
                .user(message)
                .call()
                .content();

        AiStructuredReply reply = parseStructuredReply(rawContent);
        return new AiChatResponse(
                reply.getContent(),
                sceneType.name().toLowerCase(),
                promptPlan.grounded(),
                promptPlan.nextAction(),
                promptPlan.answerType().name().toLowerCase(),
                promptPlan.card()
        );
    }

    private AiPromptPlan buildPromptByScene(AiSceneType sceneType, AiSceneRequestContext context) {
        AiSceneHandler sceneHandler = sceneHandlerMap.get(sceneType);
        if (sceneHandler == null) {
            throw new BusinessException(500, "未找到 AI 场景处理器: " + sceneType);
        }
        return sceneHandler.buildPlan(context);
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
