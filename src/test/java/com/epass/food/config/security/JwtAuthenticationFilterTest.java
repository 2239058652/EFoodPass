package com.epass.food.config.security;

import com.epass.food.modules.auth.session.entity.SysUserSession;
import com.epass.food.modules.auth.session.service.SysUserSessionService;
import com.epass.food.modules.system.permission.service.SysPermissionService;
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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysRoleService sysRoleService;

    @Mock
    private SysPermissionService sysPermissionService;

    @Mock
    private SysUserSessionService sysUserSessionService;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                sysUserService,
                sysRoleService,
                sysPermissionService,
                sysUserSessionService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterShouldSetAuthenticationWhenSessionIsValid() throws Exception {
        Claims claims = mock(Claims.class);
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setNickname("Admin");
        user.setStatus(1);
        user.setTokenVersion(2);
        SysUserSession session = new SysUserSession();
        session.setSessionId("session-1");
        session.setExpireTime(LocalDateTime.now().plusMinutes(10));

        when(jwtTokenProvider.parseToken("token-1")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("tokenVersion", Integer.class)).thenReturn(2);
        when(claims.get("sessionId", String.class)).thenReturn("session-1");
        when(sysUserService.getById(1L)).thenReturn(user);
        when(sysUserSessionService.getValidSession(1L, "session-1", 2)).thenReturn(session);
        when(sysRoleService.getRolesByUserId(1L)).thenReturn(List.of());
        when(sysPermissionService.getPermissionCodesByUserId(1L)).thenReturn(List.of());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Authorization", "Bearer token-1");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(LoginUser.class);
        verify(sysUserSessionService).touchSession(org.mockito.ArgumentMatchers.eq("session-1"), any(LocalDateTime.class));
    }

    @Test
    void doFilterShouldSkipAuthenticationWhenSessionMissing() throws Exception {
        Claims claims = mock(Claims.class);
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("tester");
        user.setStatus(1);
        user.setTokenVersion(3);

        when(jwtTokenProvider.parseToken("token-2")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("2");
        when(claims.get("tokenVersion", Integer.class)).thenReturn(3);
        when(claims.get("sessionId", String.class)).thenReturn("session-missing");
        when(sysUserService.getById(2L)).thenReturn(user);
        when(sysUserSessionService.getValidSession(2L, "session-missing", 3)).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Authorization", "Bearer token-2");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(sysUserSessionService, never()).touchSession(org.mockito.ArgumentMatchers.eq("session-missing"), any(LocalDateTime.class));
    }
}
