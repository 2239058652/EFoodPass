package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiConversationMeta;
import com.epass.food.modules.ai.dto.AiConversationSessionDetail;
import com.epass.food.modules.ai.dto.AiConversationSessionSummary;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneRequestContext;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.dto.AiStructuredReply;
import com.epass.food.modules.ai.service.AiAdvisorContextKeys;
import com.epass.food.modules.ai.service.AiChatService;
import com.epass.food.modules.ai.service.AiConversationMemoryAdvisor;
import com.epass.food.modules.ai.service.AiConversationMemoryService;
import com.epass.food.modules.ai.service.AiSceneClassifier;
import com.epass.food.modules.ai.service.AiSceneHandler;
import com.epass.food.modules.ai.service.AiStructuredOutputAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final AiSceneClassifier aiSceneClassifier;
    private final AiConversationMemoryService conversationMemoryService;
    private final Map<AiSceneType, AiSceneHandler> sceneHandlerMap;
    private final AiStructuredOutputAdvisor structuredOutputAdvisor;
    private final AiConversationMemoryAdvisor conversationMemoryAdvisor;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             AiSceneClassifier aiSceneClassifier,
                             AiConversationMemoryService conversationMemoryService,
                             List<AiSceneHandler> sceneHandlers,
                             AiStructuredOutputAdvisor structuredOutputAdvisor,
                             AiConversationMemoryAdvisor conversationMemoryAdvisor) {
        this.chatClient = chatClientBuilder.build();
        this.aiSceneClassifier = aiSceneClassifier;
        this.conversationMemoryService = conversationMemoryService;
        this.sceneHandlerMap = buildSceneHandlerMap(sceneHandlers);
        this.structuredOutputAdvisor = structuredOutputAdvisor;
        this.conversationMemoryAdvisor = conversationMemoryAdvisor;
    }

    @Override
    public AiChatResponse chat(String message, String sessionId, Long currentUserId, boolean canViewAnyOrder) {
        String resolvedSessionId = conversationMemoryService.ensureSessionId(sessionId);
        long userCreatedAt = System.currentTimeMillis();

        SceneResolution sceneResolution = resolveSceneType(message, currentUserId, resolvedSessionId);
        AiConversationMemoryService.ConversationPromptContext promptContext =
                conversationMemoryService.getPromptContext(currentUserId, resolvedSessionId);

        AiSceneRequestContext context = new AiSceneRequestContext(
                message,
                resolvedSessionId,
                currentUserId,
                canViewAnyOrder
        );
        AiPromptPlan promptPlan = buildPromptByScene(sceneResolution.sceneType(), context);

        var requestSpec = chatClient.prompt()
                .system(promptPlan.prompt())
                .user(message)
                .advisors(spec -> spec
                        .advisors(conversationMemoryAdvisor, structuredOutputAdvisor)
                        .params(buildAdvisorParams(promptPlan, promptContext)));

        if (promptPlan.hasTools()) {
            requestSpec = requestSpec.tools(promptPlan.tools());
            if (!promptPlan.toolContext().isEmpty()) {
                requestSpec = requestSpec.toolContext(promptPlan.toolContext());
            }
        }

        ResponseEntity<?, AiStructuredReply> responseEntity;
        try {
            responseEntity = requestSpec.call().responseEntity(AiStructuredReply.class);
        } catch (RuntimeException e) {
            throw new BusinessException(500, "AI 结构化输出解析失败");
        }

        long assistantCreatedAt = System.currentTimeMillis();
        AiStructuredReply reply = responseEntity.entity();
        if (reply == null || !StringUtils.hasText(reply.getContent())) {
            throw new BusinessException(500, "AI 返回的结构化内容为空");
        }

        AiAnswerType finalAnswerType = resolveAnswerType(promptPlan.answerType(), reply.getToolStatus());
        String finalToolStatus = normalizeToolStatus(reply.getToolStatus());
        String finalNextAction = resolveNextAction(promptPlan.nextAction(), finalAnswerType);
        AiDisplayCard finalCard = resolveCard(promptPlan.card(), finalAnswerType);

        conversationMemoryService.appendTurn(
                currentUserId,
                resolvedSessionId,
                sceneResolution.sceneType(),
                message,
                reply.getContent(),
                userCreatedAt,
                assistantCreatedAt
        );

        return new AiChatResponse(
                resolvedSessionId,
                reply.getContent(),
                sceneResolution.sceneType().name().toLowerCase(),
                promptPlan.grounded(),
                finalNextAction,
                finalAnswerType.name().toLowerCase(),
                finalToolStatus,
                finalCard,
                buildConversationMeta(promptContext, sceneResolution.reused())
        );
    }

    @Override
    public void clearSession(String sessionId, Long currentUserId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(400, "sessionId 不能为空");
        }
        conversationMemoryService.clearSession(currentUserId, sessionId.trim());
    }

    @Override
    public List<AiConversationSessionSummary> listSessions(Long currentUserId, int limit) {
        return conversationMemoryService.listSessions(currentUserId, limit);
    }

    @Override
    public AiConversationSessionDetail getSessionDetail(String sessionId, Long currentUserId, int pageNum, int pageSize) {
        return conversationMemoryService.getSessionDetail(currentUserId, sessionId, pageNum, pageSize);
    }

    @Override
    public void renameSession(String sessionId, String title, Long currentUserId) {
        conversationMemoryService.renameSession(currentUserId, sessionId, title);
    }

    private AiPromptPlan buildPromptByScene(AiSceneType sceneType, AiSceneRequestContext context) {
        AiSceneHandler sceneHandler = sceneHandlerMap.get(sceneType);
        if (sceneHandler == null) {
            throw new BusinessException(500, "未找到 AI 场景处理器: " + sceneType);
        }
        return sceneHandler.buildPlan(context);
    }

    private SceneResolution resolveSceneType(String message, Long currentUserId, String sessionId) {
        AiSceneType classifiedScene = aiSceneClassifier.classify(message);
        if (classifiedScene != AiSceneType.GENERAL) {
            return new SceneResolution(classifiedScene, false);
        }

        if (!shouldReuseLastScene(message)) {
            return new SceneResolution(classifiedScene, false);
        }

        return conversationMemoryService.getLastScene(currentUserId, sessionId)
                .map(sceneType -> new SceneResolution(sceneType, true))
                .orElse(new SceneResolution(AiSceneType.GENERAL, false));
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

    private Map<String, Object> buildAdvisorParams(AiPromptPlan promptPlan,
                                                   AiConversationMemoryService.ConversationPromptContext promptContext) {
        Map<String, Object> params = new HashMap<>(promptPlan.advisorParams());
        if (StringUtils.hasText(promptContext.summary())) {
            params.put(AiAdvisorContextKeys.MEMORY_SUMMARY, promptContext.summary());
        }
        if (!promptContext.recentTurns().isEmpty()) {
            params.put(AiAdvisorContextKeys.MEMORY_RECENT_TURNS, promptContext.recentTurns());
        }
        return params;
    }

    private AiConversationMeta buildConversationMeta(
            AiConversationMemoryService.ConversationPromptContext promptContext,
            boolean sceneReused
    ) {
        return new AiConversationMeta(
                StringUtils.hasText(promptContext.summary()) || !promptContext.recentTurns().isEmpty(),
                StringUtils.hasText(promptContext.summary()),
                promptContext.recentTurns().size(),
                sceneReused
        );
    }

    private AiAnswerType resolveAnswerType(AiAnswerType defaultAnswerType, String toolStatus) {
        if (!StringUtils.hasText(toolStatus)) {
            return defaultAnswerType;
        }

        return switch (toolStatus.trim().toLowerCase()) {
            case "success" -> AiAnswerType.NORMAL;
            case "not_found" -> AiAnswerType.NOT_FOUND;
            case "restricted" -> AiAnswerType.RESTRICTED;
            default -> defaultAnswerType;
        };
    }

    private String normalizeToolStatus(String toolStatus) {
        if (!StringUtils.hasText(toolStatus)) {
            return "none";
        }
        return toolStatus.trim().toLowerCase();
    }

    private String resolveNextAction(String defaultNextAction, AiAnswerType answerType) {
        if (answerType == AiAnswerType.NORMAL) {
            return defaultNextAction;
        }
        return "ask_more_details";
    }

    private AiDisplayCard resolveCard(AiDisplayCard defaultCard, AiAnswerType answerType) {
        return switch (answerType) {
            case NORMAL -> defaultCard;
            case NOT_FOUND -> new AiDisplayCard(
                    "未找到目标数据",
                    "not-found",
                    "当前查询目标不存在，请确认输入的信息是否正确。",
                    List.of()
            );
            case RESTRICTED -> new AiDisplayCard(
                    "访问受限",
                    "restricted",
                    "当前登录用户无权访问该数据，或系统不允许返回详情。",
                    List.of()
            );
        };
    }

    private Map<AiSceneType, AiSceneHandler> buildSceneHandlerMap(List<AiSceneHandler> sceneHandlers) {
        Map<AiSceneType, AiSceneHandler> handlerMap = new EnumMap<>(AiSceneType.class);
        for (AiSceneHandler sceneHandler : sceneHandlers) {
            handlerMap.put(sceneHandler.sceneType(), sceneHandler);
        }
        return handlerMap;
    }

    private record SceneResolution(AiSceneType sceneType, boolean reused) {
    }
}
