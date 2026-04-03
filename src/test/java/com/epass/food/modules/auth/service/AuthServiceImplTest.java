package com.epass.food.modules.auth.service;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.config.security.JwtTokenProvider;
import com.epass.food.modules.auth.dto.CurrentSessionResponse;
import com.epass.food.modules.auth.dto.CurrentUserChangePasswordRequest;
import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.CurrentUserUpdateRequest;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.dto.UserSessionResponse;
import com.epass.food.modules.auth.service.impl.AuthServiceImpl;
import com.epass.food.modules.auth.session.entity.SysUserSession;
import com.epass.food.modules.auth.session.service.SysUserSessionService;
import com.epass.food.modules.system.loginlog.service.SysLoginLogService;
import com.epass.food.modules.system.permission.service.SysPermissionService;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.service.SysUserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserService sysUserService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SysRoleService sysRoleService;

    @Mock
    private SysPermissionService sysPermissionService;

    @Mock
    private SysLoginLogService sysLoginLogService;

    @Mock
    private SysUserSessionService sysUserSessionService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                sysUserService,
                passwordEncoder,
                jwtTokenProvider,
                sysRoleService,
                sysPermissionService,
                sysLoginLogService,
                sysUserSessionService
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void loginShouldCreateSessionUpdateLastLoginAtAndRecordSuccess() {
        SysUser user = user(1L, "admin", "Admin", "13800000000", 0);
        user.setPasswordHash("encoded");

        SysUserSession session = session("session-1", 1L, 0);

        when(sysUserService.getByUsername("admin")).thenReturn(user);
        when(passwordEncoder.matches("Admin@123", "encoded")).thenReturn(true);
        when(jwtTokenProvider.getExpireSeconds()).thenReturn(1800L);
        when(sysUserSessionService.createSession(eq(1L), eq(0), eq("127.0.0.1"), eq("JUnit"), any(LocalDateTime.class))).thenReturn(session);
        when(jwtTokenProvider.createToken(1L, "admin", 0, "session-1")).thenReturn("token-1");

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Admin@123");

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("token-1");
        verify(sysUserService).updateById(user);
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(sysLoginLogService).recordLogin(1L, "admin", "127.0.0.1", 1, "LOGIN_SUCCESS");
    }

    @Test
    void loginShouldRecordFailureWhenPasswordInvalid() {
        SysUser user = user(2L, "tester", "Tester", null, 0);
        user.setPasswordHash("encoded");

        when(sysUserService.getByUsername("tester")).thenReturn(user);
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        LoginRequest request = new LoginRequest();
        request.setUsername("tester");
        request.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class);

        verify(sysLoginLogService).recordLogin(2L, "tester", "127.0.0.1", 0, "USERNAME_OR_PASSWORD_INVALID");
        verify(sysUserService, never()).updateById(user);
        verify(sysUserSessionService, never()).createSession(any(), any(), any(), any(), any());
    }

    @Test
    void logoutShouldIncrementTokenVersionAndClearUserSessions() {
        SysUser user = user(3L, "tester", "Tester", null, 2);
        when(sysUserService.getById(3L)).thenReturn(user);

        authService.logout(3L);

        assertThat(user.getTokenVersion()).isEqualTo(3);
        verify(sysUserService).updateById(user);
        verify(sysUserSessionService).removeUserSessions(3L);
    }

    @Test
    void updateCurrentUserShouldUpdateNicknameAndPhone() {
        SysUser user = user(4L, "admin", "Admin", "13800000000", 1);
        when(sysUserService.getById(4L)).thenReturn(user);
        when(sysUserService.getByPhone("13800138000")).thenReturn(null);
        when(sysRoleService.getRolesByUserId(4L)).thenReturn(List.of(role("ADMIN")));
        when(sysPermissionService.getPermissionCodesByUserId(4L)).thenReturn(List.of("system:user:list"));

        CurrentUserUpdateRequest request = new CurrentUserUpdateRequest();
        request.setNickname("Admin New");
        request.setPhone("13800138000");

        CurrentUserResponse response = authService.updateCurrentUser(4L, request);

        assertThat(user.getNickname()).isEqualTo("Admin New");
        assertThat(user.getPhone()).isEqualTo("13800138000");
        assertThat(response.getPhone()).isEqualTo("13800138000");
        assertThat(response.getRoleCodes()).containsExactly("ADMIN");
        verify(sysUserService).updateById(user);
    }

    @Test
    void changeCurrentUserPasswordShouldUpdatePasswordAndClearSessions() {
        SysUser user = user(5L, "admin", "Admin", null, 6);
        user.setPasswordHash("encoded-old");
        when(sysUserService.getById(5L)).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

        CurrentUserChangePasswordRequest request = new CurrentUserChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass");

        authService.changeCurrentUserPassword(5L, request);

        assertThat(user.getPasswordHash()).isEqualTo("encoded-new");
        assertThat(user.getTokenVersion()).isEqualTo(7);
        verify(sysUserService).updateById(user);
        verify(sysUserSessionService).removeUserSessions(5L);
    }

    @Test
    void changeCurrentUserPasswordShouldRejectWrongOldPassword() {
        SysUser user = user(6L, "admin", "Admin", null, 1);
        user.setPasswordHash("encoded-old");
        when(sysUserService.getById(6L)).thenReturn(user);
        when(passwordEncoder.matches("wrong-pass", "encoded-old")).thenReturn(false);

        CurrentUserChangePasswordRequest request = new CurrentUserChangePasswordRequest();
        request.setOldPassword("wrong-pass");
        request.setNewPassword("new-pass");

        assertThatThrownBy(() -> authService.changeCurrentUserPassword(6L, request))
                .isInstanceOf(BusinessException.class);

        verify(sysUserService, never()).updateById(user);
        verify(sysUserSessionService, never()).removeUserSessions(6L);
    }

    @Test
    void logoutCurrentSessionShouldRemoveCurrentSession() {
        SysUser user = user(7L, "admin", "Admin", null, 5);
        Claims claims = claims("7", "session-current");
        when(sysUserService.getById(7L)).thenReturn(user);
        when(jwtTokenProvider.parseToken("token-current")).thenReturn(claims);
        when(sysUserSessionService.getValidSession(7L, "session-current", 5)).thenReturn(session("session-current", 7L, 5));

        authService.logoutCurrentSession(7L, "token-current");

        verify(sysUserSessionService).removeSession(7L, "session-current", 5);
    }

    @Test
    void refreshTokenShouldRotateSessionAndReturnNewToken() {
        SysUser user = user(8L, "admin", "Admin", null, 6);
        Claims claims = claims("8", "session-old");
        SysUserSession oldSession = session("session-old", 8L, 6);
        SysUserSession newSession = session("session-new", 8L, 6);

        when(sysUserService.getById(8L)).thenReturn(user);
        when(jwtTokenProvider.parseToken("token-old")).thenReturn(claims);
        when(sysUserSessionService.getValidSession(8L, "session-old", 6)).thenReturn(oldSession);
        when(jwtTokenProvider.getExpireSeconds()).thenReturn(1800L);
        when(sysUserSessionService.createSession(eq(8L), eq(6), eq("127.0.0.1"), eq("JUnit"), any(LocalDateTime.class))).thenReturn(newSession);
        when(jwtTokenProvider.createToken(8L, "admin", 6, "session-new")).thenReturn("token-new");

        LoginResponse response = authService.refreshToken(8L, "token-old");

        assertThat(response.getToken()).isEqualTo("token-new");
        verify(sysUserSessionService).removeSession(8L, "session-old", 6);
    }

    @Test
    void getCurrentSessionShouldExposeSessionFieldsAndRemainingSeconds() {
        SysUser user = user(9L, "manager", "Manager", null, 7);
        Claims claims = claims("9", "session-6");
        Date issuedAt = Date.from(Instant.parse("2026-04-03T08:00:00Z"));
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(5);
        SysUserSession session = session("session-6", 9L, 7);
        session.setRequestIp("10.0.0.2");
        session.setUserAgent("Chrome");
        session.setExpireTime(expireTime);

        when(sysUserService.getById(9L)).thenReturn(user);
        when(jwtTokenProvider.parseToken("token-6")).thenReturn(claims);
        when(claims.getIssuedAt()).thenReturn(issuedAt);
        when(jwtTokenProvider.toLocalDateTime(issuedAt)).thenReturn(LocalDateTime.of(2026, 4, 3, 16, 0));
        when(sysUserSessionService.getValidSession(9L, "session-6", 7)).thenReturn(session);

        CurrentSessionResponse response = authService.getCurrentSession(9L, "token-6");

        assertThat(response.getSessionId()).isEqualTo("session-6");
        assertThat(response.getRequestIp()).isEqualTo("10.0.0.2");
        assertThat(response.getUserAgent()).isEqualTo("Chrome");
        assertThat(response.getLoginTime()).isEqualTo(session.getLoginTime());
        assertThat(response.getLastAccessTime()).isEqualTo(session.getLastAccessTime());
        assertThat(response.getRemainingSeconds()).isGreaterThan(0);
    }

    @Test
    void listSessionsShouldMarkCurrentSession() {
        SysUser user = user(10L, "operator", "Operator", null, 2);
        Claims claims = claims("10", "session-b");
        SysUserSession sessionA = session("session-a", 10L, 2);
        SysUserSession sessionB = session("session-b", 10L, 2);

        when(sysUserService.getById(10L)).thenReturn(user);
        when(jwtTokenProvider.parseToken("token-list")).thenReturn(claims);
        when(sysUserSessionService.listActiveSessions(10L, 2)).thenReturn(List.of(sessionA, sessionB));

        List<UserSessionResponse> result = authService.listSessions(10L, "token-list");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCurrent()).isFalse();
        assertThat(result.get(1).getCurrent()).isTrue();
    }

    @Test
    void offlineSessionShouldRemoveTargetSession() {
        SysUser user = user(11L, "auditor", "Auditor", null, 4);
        when(sysUserService.getById(11L)).thenReturn(user);
        when(sysUserSessionService.removeSession(11L, "session-z", 4)).thenReturn(true);

        authService.offlineSession(11L, "session-z");

        verify(sysUserSessionService).removeSession(11L, "session-z", 4);
    }

    @Test
    void offlineSessionShouldRejectMissingSession() {
        SysUser user = user(12L, "auditor", "Auditor", null, 4);
        when(sysUserService.getById(12L)).thenReturn(user);
        when(sysUserSessionService.removeSession(12L, "session-missing", 4)).thenReturn(false);

        assertThatThrownBy(() -> authService.offlineSession(12L, "session-missing"))
                .isInstanceOf(BusinessException.class);
    }

    private SysUser user(Long id, String username, String nickname, String phone, Integer tokenVersion) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setStatus(1);
        user.setTokenVersion(tokenVersion);
        return user;
    }

    private SysRole role(String roleCode) {
        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        return role;
    }

    private SysUserSession session(String sessionId, Long userId, Integer tokenVersion) {
        SysUserSession session = new SysUserSession();
        session.setUserId(userId);
        session.setSessionId(sessionId);
        session.setTokenVersion(tokenVersion);
        session.setLoginTime(LocalDateTime.of(2026, 4, 3, 10, 0));
        session.setLastAccessTime(LocalDateTime.of(2026, 4, 3, 10, 30));
        session.setExpireTime(LocalDateTime.now().plusMinutes(30));
        return session;
    }

    private Claims claims(String subject, String sessionId) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.get("sessionId", String.class)).thenReturn(sessionId);
        return claims;
    }
}
