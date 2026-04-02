package com.epass.food.modules.ai.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiConversationMessage;
import com.epass.food.modules.ai.dto.AiConversationSessionDetail;
import com.epass.food.modules.ai.dto.AiConversationSessionSummary;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AiConversationMemoryService {

    private static final int MAX_TITLE_LENGTH = 32;
    private static final int MAX_SESSION_LIST_LIMIT = 20;
    private static final int MAX_DETAIL_PAGE_SIZE = 50;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AiConversationMemoryProperties properties;

    public AiConversationMemoryService(StringRedisTemplate stringRedisTemplate,
                                       ObjectMapper objectMapper,
                                       AiConversationMemoryProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String ensureSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId.trim();
        }
        return UUID.randomUUID().toString();
    }

    public List<ConversationTurn> getRecentTurns(Long userId, String sessionId, int limit) {
        String promptTurnsKey = buildPromptTurnsKey(userId, sessionId);
        Long size = stringRedisTemplate.opsForList().size(promptTurnsKey);
        if (size == null || size == 0) {
            return List.of();
        }

        long start = Math.max(0, size - limit);
        List<String> values = stringRedisTemplate.opsForList().range(promptTurnsKey, start, -1);
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(this::deserializeTurn)
                .toList();
    }

    public ConversationPromptContext getPromptContext(Long userId, String sessionId) {
        String summary = stringRedisTemplate.opsForValue().get(buildSummaryKey(userId, sessionId));
        List<ConversationTurn> recentTurns = getRecentTurns(userId, sessionId, properties.getRecentTurnsForPrompt());
        return new ConversationPromptContext(summary, recentTurns);
    }

    public Optional<AiSceneType> getLastScene(Long userId, String sessionId) {
        String value = stringRedisTemplate.opsForValue().get(buildSceneKey(userId, sessionId));
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }

        try {
            return Optional.of(AiSceneType.valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public List<AiConversationSessionSummary> listSessions(Long userId, int limit) {
        int resolvedLimit = Math.max(1, Math.min(limit, MAX_SESSION_LIST_LIMIT));
        Set<String> sessionIds = stringRedisTemplate.opsForZSet()
                .reverseRange(buildSessionIndexKey(userId), 0, resolvedLimit - 1);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        List<AiConversationSessionSummary> sessions = new ArrayList<>();
        for (String sessionId : sessionIds) {
            AiConversationSessionSummary summary = readSessionMeta(userId, sessionId);
            if (summary != null) {
                sessions.add(summary);
            }
        }
        return sessions;
    }

    public AiConversationSessionDetail getSessionDetail(Long userId, String sessionId, int pageNum, int pageSize) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            throw new BusinessException(400, "sessionId 不能为空");
        }

        AiConversationSessionSummary sessionSummary = readSessionMeta(userId, sessionId);
        if (sessionSummary == null) {
            throw new BusinessException(404, "会话不存在");
        }

        int resolvedPageNum = Math.max(1, pageNum);
        int resolvedPageSize = Math.max(1, Math.min(pageSize, MAX_DETAIL_PAGE_SIZE));
        String summary = stringRedisTemplate.opsForValue().get(buildSummaryKey(userId, sessionId));

        PagedTurns pagedTurns = getArchiveTurnsPage(userId, sessionId, resolvedPageNum, resolvedPageSize);
        List<AiConversationMessage> messages = new ArrayList<>();
        for (ConversationTurn turn : pagedTurns.turns()) {
            messages.add(new AiConversationMessage("user", turn.userMessage(), turn.userCreatedAt()));
            messages.add(new AiConversationMessage("assistant", turn.assistantMessage(), turn.assistantCreatedAt()));
        }

        long totalMessages = pagedTurns.totalTurns() * 2;
        boolean hasMore = (long) resolvedPageNum * resolvedPageSize < totalMessages;

        return new AiConversationSessionDetail(
                sessionSummary.getSessionId(),
                sessionSummary.getTitle(),
                sessionSummary.getScene(),
                sessionSummary.getPreview(),
                sessionSummary.getUpdatedAt(),
                summary,
                totalMessages,
                resolvedPageNum,
                resolvedPageSize,
                hasMore,
                messages
        );
    }

    public void appendTurn(Long userId,
                           String sessionId,
                           AiSceneType sceneType,
                           String userMessage,
                           String assistantMessage,
                           long userCreatedAt,
                           long assistantCreatedAt) {
        String promptTurnsKey = buildPromptTurnsKey(userId, sessionId);
        String archiveTurnsKey = buildArchiveTurnsKey(userId, sessionId);
        String sceneKey = buildSceneKey(userId, sessionId);
        String summaryKey = buildSummaryKey(userId, sessionId);
        String sessionIndexKey = buildSessionIndexKey(userId);
        String sessionMetaKey = buildSessionMetaKey(userId, sessionId);
        Duration ttl = Duration.ofHours(properties.getTtlHours());

        ConversationTurn turn = new ConversationTurn(
                userMessage,
                assistantMessage,
                userCreatedAt,
                assistantCreatedAt
        );
        String serializedTurn = serializeTurn(turn);

        stringRedisTemplate.opsForList().rightPush(promptTurnsKey, serializedTurn);
        stringRedisTemplate.opsForList().trim(promptTurnsKey, -properties.getMaxTurns(), -1);
        stringRedisTemplate.expire(promptTurnsKey, ttl);

        stringRedisTemplate.opsForList().rightPush(archiveTurnsKey, serializedTurn);
        stringRedisTemplate.opsForList().trim(archiveTurnsKey, -properties.getArchiveMaxTurns(), -1);
        stringRedisTemplate.expire(archiveTurnsKey, ttl);

        stringRedisTemplate.opsForValue().set(sceneKey, sceneType.name(), ttl);
        refreshSummary(archiveTurnsKey, summaryKey, ttl);

        AiConversationSessionSummary existingSummary = readSessionMeta(userId, sessionId);
        String title = existingSummary != null
                ? existingSummary.getTitle()
                : buildInitialTitle(userMessage, sceneType);

        AiConversationSessionSummary sessionSummary = new AiConversationSessionSummary(
                sessionId,
                title,
                sceneType.name().toLowerCase(),
                buildPreview(userMessage, assistantMessage),
                assistantCreatedAt
        );
        stringRedisTemplate.opsForValue().set(sessionMetaKey, serializeSessionMeta(sessionSummary), ttl);
        stringRedisTemplate.opsForZSet().add(sessionIndexKey, sessionId, assistantCreatedAt);
        stringRedisTemplate.expire(sessionIndexKey, ttl);
    }

    public void renameSession(Long userId, String sessionId, String title) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            throw new BusinessException(400, "sessionId 不能为空");
        }
        if (!StringUtils.hasText(title)) {
            throw new BusinessException(400, "title 不能为空");
        }

        AiConversationSessionSummary existingSummary = readSessionMeta(userId, sessionId);
        if (existingSummary == null) {
            throw new BusinessException(404, "会话不存在");
        }

        AiConversationSessionSummary renamedSummary = new AiConversationSessionSummary(
                existingSummary.getSessionId(),
                normalizeTitle(title),
                existingSummary.getScene(),
                existingSummary.getPreview(),
                existingSummary.getUpdatedAt()
        );

        stringRedisTemplate.opsForValue().set(
                buildSessionMetaKey(userId, sessionId),
                serializeSessionMeta(renamedSummary),
                Duration.ofHours(properties.getTtlHours())
        );
    }

    public void clearSession(Long userId, String sessionId) {
        if (userId == null || !StringUtils.hasText(sessionId)) {
            return;
        }

        stringRedisTemplate.delete(List.of(
                buildPromptTurnsKey(userId, sessionId),
                buildArchiveTurnsKey(userId, sessionId),
                buildSceneKey(userId, sessionId),
                buildSummaryKey(userId, sessionId),
                buildSessionMetaKey(userId, sessionId)
        ));
        stringRedisTemplate.opsForZSet().remove(buildSessionIndexKey(userId), sessionId);
    }

    private PagedTurns getArchiveTurnsPage(Long userId, String sessionId, int pageNum, int pageSize) {
        String archiveTurnsKey = buildArchiveTurnsKey(userId, sessionId);
        Long totalTurns = stringRedisTemplate.opsForList().size(archiveTurnsKey);
        if (totalTurns == null || totalTurns == 0) {
            return new PagedTurns(0, List.of());
        }

        long start = (long) (pageNum - 1) * pageSize;
        if (start >= totalTurns) {
            return new PagedTurns(totalTurns, List.of());
        }

        long end = Math.min(totalTurns - 1, start + pageSize - 1);
        List<String> values = stringRedisTemplate.opsForList().range(archiveTurnsKey, start, end);
        if (values == null || values.isEmpty()) {
            return new PagedTurns(totalTurns, List.of());
        }

        List<ConversationTurn> turns = values.stream()
                .map(this::deserializeTurn)
                .toList();
        return new PagedTurns(totalTurns, turns);
    }

    private String buildPromptTurnsKey(Long userId, String sessionId) {
        return "ai:conversation:" + userId + ":" + sessionId + ":prompt-turns";
    }

    private String buildArchiveTurnsKey(Long userId, String sessionId) {
        return "ai:conversation:" + userId + ":" + sessionId + ":archive-turns";
    }

    private String buildSceneKey(Long userId, String sessionId) {
        return "ai:conversation:" + userId + ":" + sessionId + ":scene";
    }

    private String buildSummaryKey(Long userId, String sessionId) {
        return "ai:conversation:" + userId + ":" + sessionId + ":summary";
    }

    private String buildSessionMetaKey(Long userId, String sessionId) {
        return "ai:conversation:" + userId + ":" + sessionId + ":meta";
    }

    private String buildSessionIndexKey(Long userId) {
        return "ai:conversation:" + userId + ":sessions";
    }

    private void refreshSummary(String archiveTurnsKey, String summaryKey, Duration ttl) {
        List<String> values = stringRedisTemplate.opsForList().range(archiveTurnsKey, 0, -1);
        if (values == null || values.isEmpty()) {
            stringRedisTemplate.delete(summaryKey);
            return;
        }

        List<ConversationTurn> turns = values.stream()
                .map(this::deserializeTurn)
                .toList();

        int recentTurnsForPrompt = properties.getRecentTurnsForPrompt();
        if (turns.size() <= recentTurnsForPrompt) {
            stringRedisTemplate.delete(summaryKey);
            return;
        }

        List<ConversationTurn> olderTurns = new ArrayList<>(turns.subList(0, turns.size() - recentTurnsForPrompt));
        String summary = buildSummary(olderTurns);
        if (!StringUtils.hasText(summary)) {
            stringRedisTemplate.delete(summaryKey);
            return;
        }

        stringRedisTemplate.opsForValue().set(summaryKey, summary, ttl);
    }

    private String buildSummary(List<ConversationTurn> olderTurns) {
        StringBuilder summaryBuilder = new StringBuilder("较早对话摘要：");
        int maxChars = properties.getSummaryMaxChars();

        for (int i = 0; i < olderTurns.size(); i++) {
            ConversationTurn turn = olderTurns.get(i);
            String segment = "%d. 用户提到“%s”；助手回答“%s”。".formatted(
                    i + 1,
                    shorten(turn.userMessage(), 24),
                    shorten(turn.assistantMessage(), 36)
            );

            if (summaryBuilder.length() + segment.length() > maxChars) {
                summaryBuilder.append("其余更早内容已省略。");
                break;
            }
            summaryBuilder.append(segment);
        }

        return summaryBuilder.toString();
    }

    private String buildInitialTitle(String userMessage, AiSceneType sceneType) {
        String normalized = normalizeTitle(userMessage);
        if (StringUtils.hasText(normalized)) {
            return normalized;
        }
        return sceneType.name().toLowerCase() + " 对话";
    }

    private String normalizeTitle(String title) {
        String normalized = title.replace("\r", " ")
                .replace("\n", " ")
                .trim();
        if (normalized.length() <= MAX_TITLE_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_TITLE_LENGTH) + "...";
    }

    private String buildPreview(String userMessage, String assistantMessage) {
        String userPreview = shorten(userMessage, 16);
        String assistantPreview = shorten(assistantMessage, 20);
        return "用户：" + userPreview + " | 助手：" + assistantPreview;
    }

    private String shorten(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        String normalized = text.replace("\r", " ")
                .replace("\n", " ")
                .trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...";
    }

    private String serializeTurn(ConversationTurn turn) {
        try {
            return objectMapper.writeValueAsString(turn);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化会话消息失败", e);
        }
    }

    private String serializeSessionMeta(AiConversationSessionSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化会话目录失败", e);
        }
    }

    private ConversationTurn deserializeTurn(String value) {
        try {
            return objectMapper.readValue(value, ConversationTurn.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("反序列化会话消息失败", e);
        }
    }

    private AiConversationSessionSummary readSessionMeta(Long userId, String sessionId) {
        String value = stringRedisTemplate.opsForValue().get(buildSessionMetaKey(userId, sessionId));
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return objectMapper.readValue(value, AiConversationSessionSummary.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("反序列化会话目录失败", e);
        }
    }

    public record ConversationPromptContext(String summary, List<ConversationTurn> recentTurns) {
    }

    public record ConversationTurn(String userMessage,
                                   String assistantMessage,
                                   Long userCreatedAt,
                                   Long assistantCreatedAt) {
    }

    private record PagedTurns(long totalTurns, List<ConversationTurn> turns) {
    }
}
