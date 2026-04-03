package com.epass.food.modules.auth.controller;

import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.auth.dto.CurrentSessionResponse;
import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.dto.UserSessionResponse;
import com.epass.food.modules.auth.service.AuthService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void logoutShouldPassCurrentUserToService() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .principal(loginAuthentication(5L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).logout(5L);
    }

    @Test
    void refreshShouldStripBearerPrefixAndReturnServiceResult() throws Exception {
        when(authService.refreshToken(6L, "token-2"))
                .thenReturn(new LoginResponse("token-2-new", 6L, "admin", "Admin"));

        mockMvc.perform(post("/auth/refresh")
                        .principal(loginAuthentication(6L))
                        .header("Authorization", "Bearer token-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("token-2-new"))
                .andExpect(jsonPath("$.data.userId").value(6))
                .andExpect(jsonPath("$.data.username").value("admin"));

        verify(authService).refreshToken(6L, "token-2");
    }

    @Test
    void updateProfileShouldPassRequestAndReturnCurrentUser() throws Exception {
        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserId(7L);
        response.setNickname("New Nick");
        response.setPhone("13800138000");
        when(authService.updateCurrentUser(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.argThat(request ->
                request != null
                        && "New Nick".equals(request.getNickname())
                        && "13800138000".equals(request.getPhone())
        ))).thenReturn(response);

        mockMvc.perform(put("/auth/profile")
                        .principal(loginAuthentication(7L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "New Nick",
                                  "phone": "13800138000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.nickname").value("New Nick"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));

        verify(authService).updateCurrentUser(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.argThat(request ->
                request != null
                        && "New Nick".equals(request.getNickname())
                        && "13800138000".equals(request.getPhone())
        ));
    }

    @Test
    void changePasswordShouldPassRequestToService() throws Exception {
        mockMvc.perform(put("/auth/password")
                        .principal(loginAuthentication(8L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "oldPassword": "old-pass",
                                  "newPassword": "new-pass"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).changeCurrentUserPassword(org.mockito.ArgumentMatchers.eq(8L), org.mockito.ArgumentMatchers.argThat(request ->
                request != null
                        && "old-pass".equals(request.getOldPassword())
                        && "new-pass".equals(request.getNewPassword())
        ));
    }

    @Test
    void currentSessionShouldReturnServiceResult() throws Exception {
        CurrentSessionResponse response = new CurrentSessionResponse();
        response.setUserId(7L);
        response.setSessionId("session-1");
        response.setUsername("operator");
        response.setTokenVersion(3);
        response.setIssuedAt(LocalDateTime.of(2026, 4, 3, 10, 0));
        response.setExpiresAt(LocalDateTime.of(2026, 4, 3, 12, 0));
        response.setRemainingSeconds(7200L);
        when(authService.getCurrentSession(7L, "token-abc")).thenReturn(response);

        mockMvc.perform(get("/auth/session/current")
                        .principal(loginAuthentication(7L))
                        .header("Authorization", "Bearer token-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.sessionId").value("session-1"))
                .andExpect(jsonPath("$.data.username").value("operator"))
                .andExpect(jsonPath("$.data.tokenVersion").value(3))
                .andExpect(jsonPath("$.data.remainingSeconds").value(7200));

        verify(authService).getCurrentSession(7L, "token-abc");
    }

    @Test
    void listSessionsShouldReturnServiceResult() throws Exception {
        UserSessionResponse response = new UserSessionResponse();
        response.setSessionId("session-2");
        response.setCurrent(true);
        response.setRequestIp("127.0.0.1");
        when(authService.listSessions(8L, "token-list")).thenReturn(List.of(response));

        mockMvc.perform(get("/auth/session/list")
                        .principal(loginAuthentication(8L))
                        .header("Authorization", "Bearer token-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sessionId").value("session-2"))
                .andExpect(jsonPath("$.data[0].current").value(true))
                .andExpect(jsonPath("$.data[0].requestIp").value("127.0.0.1"));

        verify(authService).listSessions(8L, "token-list");
    }

    @Test
    void logoutCurrentSessionShouldPassTokenToService() throws Exception {
        mockMvc.perform(delete("/auth/session/current")
                        .principal(loginAuthentication(9L))
                        .header("Authorization", "Bearer token-current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).logoutCurrentSession(9L, "token-current");
    }

    @Test
    void offlineSessionShouldPassTargetSessionToService() throws Exception {
        mockMvc.perform(delete("/auth/session/session-9")
                        .principal(loginAuthentication(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(authService).offlineSession(10L, "session-9");
    }

    private UsernamePasswordAuthenticationToken loginAuthentication(Long userId) {
        LoginUser loginUser = new LoginUser(userId, "tester", "Tester");
        return new UsernamePasswordAuthenticationToken(loginUser, null, List.of());
    }
}
