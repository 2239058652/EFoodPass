package com.epass.food.modules.ai.service.impl;

import com.epass.food.modules.ai.dto.AiAnswerType;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.dto.AiPromptPlan;
import com.epass.food.modules.ai.dto.AiSceneType;
import com.epass.food.modules.ai.service.AiAdvisorContextKeys;
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
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    private static final String MESSAGE = "order 1 status?";
    private static final String FOLLOW_UP_MESSAGE = "then amount?";
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
        lenient().when(aiSceneClassifier.classify(MESSAGE)).thenReturn(AiSceneType.ORDER);

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
        when(callResponseSpec.chatClientResponse()).thenReturn(chatClientResponse("""
                {
                  "content": "order not found, please confirm the id",
                  "toolStatus": "not_found"
                }
                """));

        AiChatResponse response = aiChatService.chat(MESSAGE, SESSION_ID, USER_ID, false);

        assertThat(response.getAnswerType()).isEqualTo("not_found");
        assertThat(response.getToolStatus()).isEqualTo("not_found");
        assertThat(response.getNextAction()).isEqualTo("ask_more_details");
        assertThat(response.getCard().getType()).isEqualTo("not-found");
        assertThat(response.getGrounded()).isTrue();

        verify(aiMetricsService).recordChat(eq("order"), eq("not_found"), eq("not_found"), any(), anyLong());
    }

    @Test
    void chatShouldMapToolStatusRestrictedIntoStructuredResponse() {
        when(callResponseSpec.chatClientResponse()).thenReturn(chatClientResponse("""
                {
                  "content": "access denied for this order",
                  "toolStatus": "restricted"
                }
                """));

        AiChatResponse response = aiChatService.chat(MESSAGE, SESSION_ID, USER_ID, false);

        assertThat(response.getAnswerType()).isEqualTo("restricted");
        assertThat(response.getToolStatus()).isEqualTo("restricted");
        assertThat(response.getNextAction()).isEqualTo("ask_more_details");
        assertThat(response.getCard().getType()).isEqualTo("restricted");

        verify(aiMetricsService).recordChat(eq("order"), eq("restricted"), eq("restricted"), any(), anyLong());
    }

    @Test
    void chatShouldReuseLastSceneForShortFollowUpQuestion() {
        when(aiSceneClassifier.classify(FOLLOW_UP_MESSAGE)).thenReturn(AiSceneType.GENERAL);
        when(conversationMemoryService.getLastScene(USER_ID, SESSION_ID)).thenReturn(java.util.Optional.of(AiSceneType.ORDER));
        when(callResponseSpec.chatClientResponse()).thenReturn(chatClientResponse("""
                {
                  "content": "the amount is 28.00"
                }
                """));

        AiChatResponse response = aiChatService.chat(FOLLOW_UP_MESSAGE, SESSION_ID, USER_ID, false);

        assertThat(response.getScene()).isEqualTo("order");
        assertThat(response.getAnswerType()).isEqualTo("normal");
        assertThat(response.getToolStatus()).isEqualTo("none");
        assertThat(response.getNextAction()).isEqualTo("view_order_module");
        assertThat(response.getConversation()).isNotNull();
        assertThat(response.getConversation().getSceneReused()).isTrue();

        verify(conversationMemoryService).getLastScene(USER_ID, SESSION_ID);
        verify(conversationMemoryService).appendTurn(
                eq(USER_ID),
                eq(SESSION_ID),
                eq(AiSceneType.ORDER),
                eq(FOLLOW_UP_MESSAGE),
                eq("the amount is 28.00"),
                anyLong(),
                anyLong()
        );
    }

    @Test
    void chatShouldExposeUsageAndRetrievalMetadata() {
        when(aiSceneHandler.buildPlan(any())).thenReturn(new AiPromptPlan(
                "system rag prompt",
                AiAnswerType.NORMAL,
                true,
                "view_system_module",
                new AiDisplayCard("System Modules", "system-modules", "Test card", List.of()),
                new Object[0],
                Map.of(),
                Map.of(
                        AiAdvisorContextKeys.RAG_KNOWLEDGE_BASE, "system",
                        AiAdvisorContextKeys.RAG_FILTER_EXPRESSION, "moduleCode == 'system'",
                        AiAdvisorContextKeys.RAG_TOP_K, 4,
                        AiAdvisorContextKeys.RAG_SIMILARITY_THRESHOLD, 0.75
                )
        ));
        when(callResponseSpec.chatClientResponse()).thenReturn(chatClientResponse(
                """
                {
                  "content": "permission control is handled by Spring Security",
                  "toolStatus": "success"
                }
                """,
                ChatResponseMetadata.builder()
                        .id("resp-1")
                        .model("qwen-plus")
                        .usage(new DefaultUsage(120, 45, 165))
                        .build(),
                Map.of(
                        QuestionAnswerAdvisor.RETRIEVED_DOCUMENTS,
                        List.of(
                                Document.builder()
                                        .id("doc-1")
                                        .text("System permission control relies on Spring Security and @PreAuthorize annotations.")
                                        .metadata("title", "Permission Control")
                                        .score(0.91)
                                        .build()
                        )
                )
        ));

        AiChatResponse response = aiChatService.chat(MESSAGE, SESSION_ID, USER_ID, false);

        assertThat(response.getToolStatus()).isEqualTo("success");
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getResponseId()).isEqualTo("resp-1");
        assertThat(response.getUsage().getModel()).isEqualTo("qwen-plus");
        assertThat(response.getUsage().getPromptTokens()).isEqualTo(120);
        assertThat(response.getUsage().getCompletionTokens()).isEqualTo(45);
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(165);

        assertThat(response.getRetrieval()).isNotNull();
        assertThat(response.getRetrieval().isRetrievalApplied()).isTrue();
        assertThat(response.getRetrieval().getKnowledgeBase()).isEqualTo("system");
        assertThat(response.getRetrieval().getFilterExpression()).isEqualTo("moduleCode == 'system'");
        assertThat(response.getRetrieval().getTopK()).isEqualTo(4);
        assertThat(response.getRetrieval().getSimilarityThreshold()).isEqualTo(0.75);
        assertThat(response.getRetrieval().getRetrievedCount()).isEqualTo(1);
        assertThat(response.getRetrieval().getDocuments()).hasSize(1);
        assertThat(response.getRetrieval().getDocuments().get(0).getId()).isEqualTo("doc-1");
        assertThat(response.getRetrieval().getDocuments().get(0).getTitle()).isEqualTo("Permission Control");
        assertThat(response.getRetrieval().getDocuments().get(0).getScore()).isEqualTo(0.91);
        assertThat(response.getRetrieval().getDocuments().get(0).getSnippet()).contains("Spring Security");

        verify(aiMetricsService).recordChat(eq("order"), eq("normal"), eq("success"), any(), anyLong());
    }

    private ChatClientResponse chatClientResponse(String json) {
        return new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage(json)))),
                Map.of()
        );
    }

    private ChatClientResponse chatClientResponse(String json,
                                                  ChatResponseMetadata metadata,
                                                  Map<String, Object> context) {
        return new ChatClientResponse(
                new ChatResponse(List.of(new Generation(new AssistantMessage(json))), metadata),
                context
        );
    }
}
