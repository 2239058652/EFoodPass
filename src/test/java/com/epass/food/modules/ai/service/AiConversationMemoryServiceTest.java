package com.epass.food.modules.ai.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiConversationMessage;
import com.epass.food.modules.ai.dto.AiConversationSessionDetail;
import com.epass.food.modules.ai.dto.AiConversationSessionSummary;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiConversationMemoryServiceTest {

    private static final long USER_ID = 1L;
    private static final String SESSION_ID = "session-1";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    private AiConversationMemoryService conversationMemoryService;
    private AiConversationMemoryProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AiConversationMemoryProperties();
        properties.setTtlHours(12);

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);

        conversationMemoryService = new AiConversationMemoryService(
                stringRedisTemplate,
                objectMapper,
                properties
        );
    }

    @Test
    void getSessionDetailShouldReturnLatestMessagesOnFirstPage() throws Exception {
        AiConversationSessionSummary summary = new AiConversationSessionSummary(
                SESSION_ID,
                "Order Follow Up",
                "order",
                "latest preview",
                3000L
        );
        List<String> serializedTurns = List.of(
                objectMapper.writeValueAsString(new AiConversationMemoryService.ConversationTurn(
                        "u-1", "a-1", "first question", "first answer", 1000L, 1100L
                )),
                objectMapper.writeValueAsString(new AiConversationMemoryService.ConversationTurn(
                        "u-2", "a-2", "second question", "second answer", 2000L, 2100L
                )),
                objectMapper.writeValueAsString(new AiConversationMemoryService.ConversationTurn(
                        null, null, "third question", "third answer", 3000L, 3100L
                ))
        );

        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            if (key.endsWith(":meta")) {
                return objectMapper.writeValueAsString(summary);
            }
            if (key.endsWith(":summary")) {
                return "Older conversation summary";
            }
            return null;
        });
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            if (key.endsWith(":archive-turns")) {
                return serializedTurns;
            }
            return List.of();
        });

        AiConversationSessionDetail detail = conversationMemoryService.getSessionDetail(USER_ID, SESSION_ID, 1, 2);

        assertThat(detail.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(detail.getSummary()).isEqualTo("Older conversation summary");
        assertThat(detail.getTotalMessages()).isEqualTo(6);
        assertThat(detail.getPageNum()).isEqualTo(1);
        assertThat(detail.getPageSize()).isEqualTo(2);
        assertThat(detail.getHasMore()).isTrue();
        assertThat(detail.getMessages()).hasSize(2);

        AiConversationMessage userMessage = detail.getMessages().get(0);
        AiConversationMessage assistantMessage = detail.getMessages().get(1);

        assertThat(userMessage.getRole()).isEqualTo("user");
        assertThat(userMessage.getContent()).isEqualTo("third question");
        assertThat(userMessage.getId()).isEqualTo("session-1-user-2");

        assertThat(assistantMessage.getRole()).isEqualTo("assistant");
        assertThat(assistantMessage.getContent()).isEqualTo("third answer");
        assertThat(assistantMessage.getId()).isEqualTo("session-1-assistant-2");
    }

    @Test
    void renameSessionShouldOnlyUpdateTitleAndPreserveOtherMetadata() throws Exception {
        AiConversationSessionSummary existingSummary = new AiConversationSessionSummary(
                SESSION_ID,
                "Old Title",
                "order",
                "existing preview",
                4567L
        );
        when(valueOperations.get(anyString())).thenReturn(objectMapper.writeValueAsString(existingSummary));

        conversationMemoryService.renameSession(USER_ID, SESSION_ID, "Renamed order session title");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        assertThat(keyCaptor.getValue()).endsWith(":meta");
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(12));

        AiConversationSessionSummary renamedSummary = objectMapper.readValue(
                valueCaptor.getValue(),
                AiConversationSessionSummary.class
        );
        assertThat(renamedSummary.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(renamedSummary.getTitle()).isEqualTo("Renamed order session title");
        assertThat(renamedSummary.getScene()).isEqualTo("order");
        assertThat(renamedSummary.getPreview()).isEqualTo("existing preview");
        assertThat(renamedSummary.getUpdatedAt()).isEqualTo(4567L);
    }

    @Test
    void clearSessionShouldDeleteSessionKeysAndRemoveSessionIndex() {
        conversationMemoryService.clearSession(USER_ID, SESSION_ID);

        ArgumentCaptor<List<String>> deleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).delete(deleteCaptor.capture());
        verify(zSetOperations).remove("ai:conversation:1:sessions", SESSION_ID);

        assertThat(deleteCaptor.getValue()).containsExactly(
                "ai:conversation:1:session-1:prompt-turns",
                "ai:conversation:1:session-1:archive-turns",
                "ai:conversation:1:session-1:scene",
                "ai:conversation:1:session-1:summary",
                "ai:conversation:1:session-1:meta"
        );
    }

    @Test
    void appendTurnShouldUpdateSummaryMetaAndSessionIndex() throws Exception {
        properties.setRecentTurnsForPrompt(2);

        List<String> archivedTurnsAfterAppend = List.of(
                objectMapper.writeValueAsString(new AiConversationMemoryService.ConversationTurn(
                        "u-1", "a-1", "first user question", "first assistant answer", 1000L, 1100L
                )),
                objectMapper.writeValueAsString(new AiConversationMemoryService.ConversationTurn(
                        "u-2", "a-2", "second user question", "second assistant answer", 2000L, 2100L
                )),
                objectMapper.writeValueAsString(new AiConversationMemoryService.ConversationTurn(
                        "u-3", "a-3", "third user question", "third assistant answer", 3000L, 3100L
                ))
        );

        when(valueOperations.get(anyString())).thenReturn(null);
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            if (key.endsWith(":archive-turns")) {
                return archivedTurnsAfterAppend;
            }
            return List.of();
        });

        conversationMemoryService.appendTurn(
                USER_ID,
                SESSION_ID,
                AiSceneType.ORDER,
                "latest order question",
                "latest order answer",
                4000L,
                4100L
        );

        verify(listOperations).rightPush(eq("ai:conversation:1:session-1:prompt-turns"), anyString());
        verify(listOperations).rightPush(eq("ai:conversation:1:session-1:archive-turns"), anyString());
        verify(listOperations).trim("ai:conversation:1:session-1:prompt-turns", -properties.getMaxTurns(), -1);
        verify(listOperations).trim("ai:conversation:1:session-1:archive-turns", -properties.getArchiveMaxTurns(), -1);
        verify(zSetOperations).add("ai:conversation:1:sessions", SESSION_ID, 4100L);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations, org.mockito.Mockito.atLeast(3)).set(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

        List<String> writtenKeys = keyCaptor.getAllValues();
        assertThat(writtenKeys).contains(
                "ai:conversation:1:session-1:scene",
                "ai:conversation:1:session-1:summary",
                "ai:conversation:1:session-1:meta"
        );
        assertThat(ttlCaptor.getAllValues()).allMatch(duration -> duration.equals(Duration.ofHours(12)));

        int summaryIndex = writtenKeys.indexOf("ai:conversation:1:session-1:summary");
        assertThat(valueCaptor.getAllValues().get(summaryIndex)).isNotBlank();

        int metaIndex = writtenKeys.lastIndexOf("ai:conversation:1:session-1:meta");
        AiConversationSessionSummary summary = objectMapper.readValue(
                valueCaptor.getAllValues().get(metaIndex),
                AiConversationSessionSummary.class
        );
        assertThat(summary.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(summary.getScene()).isEqualTo("order");
        assertThat(summary.getUpdatedAt()).isEqualTo(4100L);
        assertThat(summary.getTitle()).isEqualTo("latest order question");
        assertThat(summary.getPreview()).isNotBlank();
    }

    @Test
    void listSessionsShouldRespectLimitAndKeepLatestOrder() throws Exception {
        AiConversationSessionSummary summary3 = new AiConversationSessionSummary("session-3", "Title 3", "system", "preview-3", 3000L);
        AiConversationSessionSummary summary2 = new AiConversationSessionSummary("session-2", "Title 2", "item", "preview-2", 2000L);
        AiConversationSessionSummary summary1 = new AiConversationSessionSummary("session-1", "Title 1", "order", "preview-1", 1000L);

        when(zSetOperations.reverseRange("ai:conversation:1:sessions", 0, 19))
                .thenReturn(new LinkedHashSet<>(List.of("session-3", "session-2", "session-1")));
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            if (key.endsWith("session-3:meta")) {
                return objectMapper.writeValueAsString(summary3);
            }
            if (key.endsWith("session-2:meta")) {
                return objectMapper.writeValueAsString(summary2);
            }
            if (key.endsWith("session-1:meta")) {
                return objectMapper.writeValueAsString(summary1);
            }
            return null;
        });

        List<AiConversationSessionSummary> sessions = conversationMemoryService.listSessions(USER_ID, 99);

        verify(zSetOperations).reverseRange("ai:conversation:1:sessions", 0, 19);
        assertThat(sessions).extracting(AiConversationSessionSummary::getSessionId)
                .containsExactly("session-3", "session-2", "session-1");
        assertThat(sessions).extracting(AiConversationSessionSummary::getUpdatedAt)
                .containsExactly(3000L, 2000L, 1000L);
    }

    @Test
    void getLastSceneShouldReturnEmptyWhenStoredValueIsInvalid() {
        when(valueOperations.get("ai:conversation:1:session-1:scene")).thenReturn("INVALID_SCENE");

        Optional<AiSceneType> scene = conversationMemoryService.getLastScene(USER_ID, SESSION_ID);

        assertThat(scene).isEmpty();
    }

    @Test
    void getSessionDetailShouldThrowWhenSessionMetaDoesNotExist() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> conversationMemoryService.getSessionDetail(USER_ID, SESSION_ID, 1, 20))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("会话");
    }
}
