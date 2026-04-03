package com.epass.food.config.security;

import com.epass.food.modules.auth.session.entity.SysUserSession;
import com.epass.food.modules.auth.session.service.SysUserSessionService;
import com.epass.food.modules.system.permission.service.SysPermissionService;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.service.SysUserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;
    private final SysUserSessionService sysUserSessionService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   SysUserService sysUserService,
                                   SysRoleService sysRoleService,
                                   SysPermissionService sysPermissionService,
                                   SysUserSessionService sysUserSessionService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserService = sysUserService;
        this.sysRoleService = sysRoleService;
        this.sysPermissionService = sysPermissionService;
        this.sysUserSessionService = sysUserSessionService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.parseToken(token);
                Long userId = Long.valueOf(claims.getSubject());
                Integer tokenVersion = claims.get("tokenVersion", Integer.class);
                String sessionId = claims.get("sessionId", String.class);

                SysUser user = sysUserService.getById(userId);
                SysUserSession session = sysUserSessionService.getValidSession(userId, sessionId, tokenVersion);
                if (user != null
                        && Integer.valueOf(1).equals(user.getStatus())
                        && Objects.equals(user.getTokenVersion(), tokenVersion)
                        && session != null) {
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    List<SysRole> roleList = sysRoleService.getRolesByUserId(userId);
                    for (SysRole role : roleList) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));
                    }

                    List<String> permissionCodes = sysPermissionService.getPermissionCodesByUserId(userId);
                    for (String permissionCode : permissionCodes) {
                        authorities.add(new SimpleGrantedAuthority(permissionCode));
                    }

                    sysUserSessionService.touchSession(sessionId, LocalDateTime.now());

                    LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getNickname());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
