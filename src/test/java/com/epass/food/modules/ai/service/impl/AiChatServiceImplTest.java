package com.epass.food.modules.ai.service.impl;

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

import java.util.List;
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
                com.epass.food.modules.ai.dto.AiAnswerType.NORMAL,
                true,
                "view_order_module",
                new AiDisplayCard("订单助手", "order", "测试卡片", List.of())
        ));

        when(conversationMemoryService.ensureSessionId("session-1")).thenReturn("session-1");
        when(conversationMemoryService.getPromptContext(1L, "session-1"))
                .thenReturn(new AiConversationMemoryService.ConversationPromptContext(null, List.of()));
        when(aiSceneClassifier.classify("订单 1 是什么状态？")).thenReturn(AiSceneType.ORDER);

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

        AiChatResponse response = aiChatService.chat("订单 1 是什么状态？", "session-1", 1L, false);

        assertThat(response.getSessionId()).isEqualTo("session-1");
        assertThat(response.getScene()).isEqualTo("order");
        assertThat(response.getAnswerType()).isEqualTo("degraded");
        assertThat(response.getToolStatus()).isEqualTo("degraded");
        assertThat(response.getGrounded()).isFalse();
        assertThat(response.getNextAction()).isEqualTo("ask_more_details");
        assertThat(response.getCard().getType()).isEqualTo("fallback");
        assertThat(response.getContent()).contains("降级");

        verify(conversationMemoryService).appendTurn(
                eq(1L),
                eq("session-1"),
                eq(AiSceneType.ORDER),
                eq("订单 1 是什么状态？"),
                any(String.class),
                anyLong(),
                anyLong()
        );
        verify(aiMetricsService).recordFallback("order", "model_call");
        verify(aiMetricsService).recordChat(eq("order"), eq("degraded"), eq("degraded"), any(), anyLong());
    }
}
