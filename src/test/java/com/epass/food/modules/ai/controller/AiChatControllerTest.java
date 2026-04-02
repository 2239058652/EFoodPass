package com.epass.food.modules.ai.controller;

import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiConversationSessionDetail;
import com.epass.food.modules.ai.dto.AiConversationSessionSummary;
import com.epass.food.modules.ai.dto.AiDisplayCard;
import com.epass.food.modules.ai.service.AiChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiChatControllerTest {

    @Mock
    private AiChatService aiChatService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().findAndAddModules().build();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AiChatController controller = new AiChatController(aiChatService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    void chatShouldPassAuthenticatedUserAndPermissionFlag() throws Exception {
        AiChatResponse response = new AiChatResponse(
                "session-1",
                "order answer",
                "order",
                true,
                "view_order_detail",
                "normal",
                "success",
                new AiDisplayCard("Order Detail", "order-detail", "detail card", List.of()),
                null,
                null,
                null
        );
        when(aiChatService.chat("order 1 status?", "session-1", 7L, true)).thenReturn(response);

        mockMvc.perform(post("/ai/chat")
                        .principal(loginAuthentication(7L, "food:order:detail"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "order 1 status?",
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"))
                .andExpect(jsonPath("$.data.scene").value("order"))
                .andExpect(jsonPath("$.data.nextAction").value("view_order_detail"));

        verify(aiChatService).chat("order 1 status?", "session-1", 7L, true);
    }

    @Test
    void chatShouldRejectBlankMessage() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .principal(loginAuthentication(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "   ",
                                  "sessionId": "session-1"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiChatService);
    }

    @Test
    void listSessionsShouldPassLimitAndCurrentUser() throws Exception {
        when(aiChatService.listSessions(8L, 5)).thenReturn(List.of(
                new AiConversationSessionSummary("session-2", "Order Session", "order", "preview", 1234L)
        ));

        mockMvc.perform(get("/ai/chat/sessions")
                        .principal(loginAuthentication(8L))
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").value("session-2"))
                .andExpect(jsonPath("$.data[0].title").value("Order Session"));

        verify(aiChatService).listSessions(8L, 5);
    }

    @Test
    void getSessionDetailShouldPassPagingParameters() throws Exception {
        when(aiChatService.getSessionDetail("session-3", 9L, 2, 10)).thenReturn(
                new AiConversationSessionDetail(
                        "session-3",
                        "System Session",
                        "system",
                        "preview",
                        5678L,
                        "summary",
                        24L,
                        2,
                        10,
                        true,
                        List.of()
                )
        );

        mockMvc.perform(get("/ai/chat/session/session-3")
                        .principal(loginAuthentication(9L))
                        .param("pageNum", "2")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value("session-3"))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.hasMore").value(true));

        verify(aiChatService).getSessionDetail("session-3", 9L, 2, 10);
    }

    @Test
    void renameSessionShouldPassTitleToService() throws Exception {
        mockMvc.perform(put("/ai/chat/session/session-9/title")
                        .principal(loginAuthentication(11L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Renamed Session"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(aiChatService).renameSession("session-9", "Renamed Session", 11L);
    }

    private UsernamePasswordAuthenticationToken loginAuthentication(Long userId, String... authorities) {
        LoginUser loginUser = new LoginUser(userId, "tester", "Tester");
        List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities)
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new UsernamePasswordAuthenticationToken(loginUser, null, grantedAuthorities);
    }
}
