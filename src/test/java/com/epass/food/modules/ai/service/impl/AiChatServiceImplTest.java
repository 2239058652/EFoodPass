package com.epass.food.modules.ai.service.impl;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.service.AiConversationMemoryAdvisor;
import com.epass.food.modules.ai.service.AiConversationMemoryService;
import com.epass.food.modules.ai.service.AiMetricsService;
import com.epass.food.modules.ai.service.AiSceneClassifier;
import com.epass.food.modules.ai.service.AiSceneHandler;
import com.epass.food.modules.ai.service.AiStructuredOutputAdvisor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    private static final String MESSAGE = "order 1 status?";
    private static final String SESSION_ID = "session-1";
    private static final long USER_ID = 1L;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private AiSceneClassifier aiSceneClassifier;

    @Mock
    private AiConversationMemoryService conversationMemoryService;

    @Mock
    private AiSceneHandler aiSceneHandler;

    @Mock
    private AiStructuredOutputAdvisor structuredOutputAdvisor;

    @Mock
    private AiConversationMemoryAdvisor conversationMemoryAdvisor;

    @Mock
    private AiMetricsService aiMetricsService;

    private AiChatServiceImpl aiChatService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(any(String.class))).thenReturn(requestSpec);
        doReturn(requestSpec).when(requestSpec).advisors(any(Consumer.class));
        when(requestSpec.call()).thenReturn(callResponseSpec);

        when(aiSceneHandler.sceneType()).thenReturn(AiSceneType.ORDER);
        when(aiSceneHandler.buildPlan(any())).thenReturn(new AiPromptPlan(
                "test prompt",
                AiAnswerType.NORMAL,
                true,
                "view_order_module",
                new AiDisplayCard("Order Assistant", "order", "Test card", List.of())
        ));

        when(conversationMemoryService.ensureSessionId(SESSION_ID)).thenReturn(SESSION_ID);
        when(conversationMemoryService.getPromptContext(USER_ID, SESSION_ID))
                .thenReturn(new AiConversationMemoryService.ConversationPromptContext(null, List.of()));
        when(aiSceneClassifier.classify(MESSAGE)).thenReturn(AiSceneType.ORDER);

        aiChatService = new AiChatServiceImpl(
                chatClientBuilder,
                aiSceneClassifier,
                conversationMemoryService,
                List.of(aiSceneHandler),
                structuredOutputAdvisor,
                conversationMemoryAdvisor,
                aiMetricsService
        );
    }

    @Test
    void chatShouldReturnDegradedResponseWhenModelCallFails() {
        when(callResponseSpec.chatClientResponse()).thenThrow(new RuntimeException("model down"));

        AiChatResponse response = aiChatService.chat(MESSAGE, SESSION_ID, USER_ID, false);

        assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(response.getScene()).isEqualTo("order");
        assertThat(response.getAnswerType()).isEqualTo("degraded");
        assertThat(response.getToolStatus()).isEqualTo("degraded");
        assertThat(response.getGrounded()).isFalse();
        assertThat(response.getNextAction()).isEqualTo("ask_more_details");
        assertThat(response.getCard().getType()).isEqualTo("fallback");
        assertThat(response.getContent()).isNotBlank();

        verify(conversationMemoryService).appendTurn(
                eq(USER_ID),
                eq(SESSION_ID),
                eq(AiSceneType.ORDER),
                eq(MESSAGE),
                any(String.class),
                anyLong(),
                anyLong()
        );
        verify(aiMetricsService).recordFallback("order", "model_call");
        verify(aiMetricsService).recordChat(eq("order"), eq("degraded"), eq("degraded"), any(), anyLong());
    }

    @Test
    void chatShouldMapToolStatusNotFoundIntoStructuredResponse() {
        String json = """
                {
                  "content": "order not found, please confirm the id",
                  "toolStatus": "not_found"
                }
                """;
        ChatClientResponse chatClientResponse = new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage(json)))),
                Map.of()
        );
        when(callResponseSpec.chatClientResponse()).thenReturn(chatClientResponse);

        AiChatResponse response = aiChatService.chat(MESSAGE, SESSION_ID, USER_ID, false);

        assertThat(response.getAnswerType()).isEqualTo("not_found");
        assertThat(response.getToolStatus()).isEqualTo("not_found");
        assertThat(response.getNextAction()).isEqualTo("ask_more_details");
        assertThat(response.getCard().getType()).isEqualTo("not-found");
        assertThat(response.getGrounded()).isTrue();

        verify(aiMetricsService).recordChat(eq("order"), eq("not_found"), eq("not_found"), any(), anyLong());
    }
}
