package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiChatStreamChunk;
import com.epass.food.modules.ai.dto.AiConversationMeta;
import com.epass.food.modules.ai.dto.AiConversationSessionDetail;
import com.epass.food.modules.ai.dto.AiConversationSessionSummary;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiModelUsage;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiRetrievalMeta;
import com.epass.food.modules.ai.dto.AiRetrievedDocument;
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
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        SceneRuntime runtime = prepareRuntime(message, resolvedSessionId, currentUserId, canViewAnyOrder);
        var requestSpec = buildStructuredRequest(runtime);

        ChatClientResponse chatClientResponse;
        AiStructuredReply reply;
        try {
            chatClientResponse = requestSpec.call().chatClientResponse();
            reply = new BeanOutputConverter<>(AiStructuredReply.class)
                    .convert(extractAssistantText(chatClientResponse.chatResponse()));
        } catch (RuntimeException e) {
            throw new BusinessException(500, "AI 结构化输出解析失败");
        }

        long assistantCreatedAt = System.currentTimeMillis();
        if (reply == null || !StringUtils.hasText(reply.getContent())) {
            throw new BusinessException(500, "AI 返回的结构化内容为空");
        }

        AiAnswerType finalAnswerType = resolveAnswerType(runtime.promptPlan.answerType(), reply.getToolStatus());
        String finalToolStatus = normalizeToolStatus(reply.getToolStatus());
        String finalNextAction = resolveNextAction(runtime.promptPlan.nextAction(), finalAnswerType);
        AiDisplayCard finalCard = resolveCard(runtime.promptPlan.card(), finalAnswerType);
        AiModelUsage usage = extractUsage(chatClientResponse.chatResponse());
        AiRetrievalMeta retrievalMeta = extractRetrievalMeta(chatClientResponse);

        conversationMemoryService.appendTurn(
                currentUserId,
                resolvedSessionId,
                runtime.sceneResolution.sceneType(),
                message,
                reply.getContent(),
                userCreatedAt,
                assistantCreatedAt
        );

        return new AiChatResponse(
                resolvedSessionId,
                reply.getContent(),
                runtime.sceneResolution.sceneType().name().toLowerCase(),
                runtime.promptPlan.grounded(),
                finalNextAction,
                finalAnswerType.name().toLowerCase(),
                finalToolStatus,
                finalCard,
                usage,
                buildConversationMeta(runtime.promptContext, runtime.sceneResolution.reused()),
                retrievalMeta
        );
    }

    @Override
    public Flux<AiChatStreamChunk> streamChat(String message, String sessionId, Long currentUserId, boolean canViewAnyOrder) {
        String resolvedSessionId = conversationMemoryService.ensureSessionId(sessionId);
        long userCreatedAt = System.currentTimeMillis();

        SceneRuntime runtime = prepareRuntime(message, resolvedSessionId, currentUserId, canViewAnyOrder);
        String streamPrompt = runtime.promptPlan.prompt() + """

                本次是流式输出。
                直接连续输出最终给用户看的中文回答正文，不要输出 JSON，不要输出字段名，不要解释输出格式。
                """;

        var requestSpec = buildBaseRequest(runtime, streamPrompt)
                .advisors(spec -> spec
                        .advisors(buildStreamAdvisors(runtime))
                        .params(buildMemoryAdvisorParams(runtime.promptContext)));

        StringBuilder assistantContent = new StringBuilder();
        String scene = runtime.sceneResolution.sceneType().name().toLowerCase();

        Flux<AiChatStreamChunk> prefix = Flux.just(new AiChatStreamChunk(resolvedSessionId, scene, "", false));
        Flux<AiChatStreamChunk> contentFlux = requestSpec.stream()
                .content()
                .doOnNext(assistantContent::append)
                .map(delta -> new AiChatStreamChunk(resolvedSessionId, scene, delta, false))
                .doOnComplete(() -> conversationMemoryService.appendTurn(
                        currentUserId,
                        resolvedSessionId,
                        runtime.sceneResolution.sceneType(),
                        message,
                        assistantContent.toString(),
                        userCreatedAt,
                        System.currentTimeMillis()
                ));
        Flux<AiChatStreamChunk> suffix = Flux.just(new AiChatStreamChunk(resolvedSessionId, scene, "", true));

        return Flux.concat(prefix, contentFlux, suffix);
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

    private SceneRuntime prepareRuntime(String message,
                                        String sessionId,
                                        Long currentUserId,
                                        boolean canViewAnyOrder) {
        SceneResolution sceneResolution = resolveSceneType(message, currentUserId, sessionId);
        AiConversationMemoryService.ConversationPromptContext promptContext =
                conversationMemoryService.getPromptContext(currentUserId, sessionId);

        AiSceneRequestContext context = new AiSceneRequestContext(
                message,
                sessionId,
                currentUserId,
                canViewAnyOrder
        );
        AiPromptPlan promptPlan = buildPromptByScene(sceneResolution.sceneType(), context);
        return new SceneRuntime(message, sceneResolution, promptContext, promptPlan);
    }

    private ChatClient.ChatClientRequestSpec buildStructuredRequest(SceneRuntime runtime) {
        return buildBaseRequest(runtime, runtime.promptPlan.prompt())
                .advisors(spec -> spec
                        .advisors(buildStructuredAdvisors(runtime))
                        .params(buildAdvisorParams(runtime.promptPlan, runtime.promptContext)));
    }

    private ChatClient.ChatClientRequestSpec buildBaseRequest(SceneRuntime runtime, String systemPrompt) {
        var requestSpec = chatClient.prompt()
                .system(systemPrompt)
                .user(runtime.message());

        if (runtime.promptPlan.hasTools()) {
            requestSpec = requestSpec.tools(runtime.promptPlan.tools());
            if (!runtime.promptPlan.toolContext().isEmpty()) {
                requestSpec = requestSpec.toolContext(runtime.promptPlan.toolContext());
            }
        }
        return requestSpec;
    }

    private Advisor[] buildStructuredAdvisors(SceneRuntime runtime) {
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(conversationMemoryAdvisor);
        advisors.add(structuredOutputAdvisor);
        if (runtime.promptPlan.hasAdvisors()) {
            advisors.addAll(List.of(runtime.promptPlan.advisors()));
        }
        return advisors.toArray(Advisor[]::new);
    }

    private Advisor[] buildStreamAdvisors(SceneRuntime runtime) {
        List<Advisor> advisors = new ArrayList<>();
        advisors.add(conversationMemoryAdvisor);
        if (runtime.promptPlan.hasAdvisors()) {
            advisors.addAll(List.of(runtime.promptPlan.advisors()));
        }
        return advisors.toArray(Advisor[]::new);
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
        Map<String, Object> params = buildMemoryAdvisorParams(promptContext);
        params.putAll(promptPlan.advisorParams());
        return params;
    }

    private Map<String, Object> buildMemoryAdvisorParams(AiConversationMemoryService.ConversationPromptContext promptContext) {
        Map<String, Object> params = new HashMap<>();
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

    private String extractAssistantText(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null || chatResponse.getResult().getOutput() == null) {
            return null;
        }
        return chatResponse.getResult().getOutput().getText();
    }

    private AiModelUsage extractUsage(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return null;
        }

        ChatResponseMetadata metadata = chatResponse.getMetadata();
        if (metadata == null) {
            return null;
        }

        Usage usage = metadata.getUsage();
        return new AiModelUsage(
                metadata.getId(),
                metadata.getModel(),
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens()
        );
    }

    private AiRetrievalMeta extractRetrievalMeta(ChatClientResponse chatClientResponse) {
        if (chatClientResponse == null || chatClientResponse.context() == null) {
            return null;
        }

        Object retrieved = chatClientResponse.context().get(QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS);
        if (!(retrieved instanceof List<?> retrievedDocuments)) {
            return new AiRetrievalMeta(false, 0, List.of());
        }

        List<AiRetrievedDocument> documents = retrievedDocuments.stream()
                .filter(Document.class::isInstance)
                .map(Document.class::cast)
                .map(this::mapRetrievedDocument)
                .filter(Objects::nonNull)
                .toList();

        return new AiRetrievalMeta(!documents.isEmpty(), documents.size(), documents);
    }

    private AiRetrievedDocument mapRetrievedDocument(Document document) {
        if (document == null) {
            return null;
        }

        String title = valueAsString(document.getMetadata().getOrDefault("title",
                document.getMetadata().getOrDefault("documentId", document.getId())));
        String snippet = buildSnippet(document.getText());
        return new AiRetrievedDocument(
                document.getId(),
                title,
                document.getScore(),
                snippet
        );
    }

    private String valueAsString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String buildSnippet(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= 120 ? normalized : normalized.substring(0, 120) + "...";
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

    private record SceneRuntime(
            String message,
            SceneResolution sceneResolution,
            AiConversationMemoryService.ConversationPromptContext promptContext,
            AiPromptPlan promptPlan
    ) {
    }
}
