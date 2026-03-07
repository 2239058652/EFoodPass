package com.epass.food.config.security;

import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.service.SysUserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器 （每次请求先经过过滤器链，这个jwt过滤器是其中一环）
 *
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, SysUserService sysUserService,
                                   SysRoleService sysRoleService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysUserService = sysUserService;
        this.sysRoleService = sysRoleService;
    }

    /**
     * 获取请求中的 JWT token，并解析，设置用户认证信息
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtTokenProvider.parseToken(token);
                Long userId = Long.valueOf(claims.getSubject());
                Integer tokenVersion = claims.get("tokenVersion", Integer.class);

                SysUser user = sysUserService.getById(userId);
                if (user != null && Integer.valueOf(1).equals(user.getStatus())
                        && user.getTokenVersion().equals(tokenVersion)) {

                    // 先准备一个空列表，用来装“Security 能识别的角色标签
                    List<SysRole> roleList = sysRoleService.getRolesByUserId(userId);

                    // 遍历角色列表，将角色标签装入 authorities 列表
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    for (SysRole role : roleList) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));
                    }

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

    /**
     * 获取请求中的 JWT token
     *
     * @param request 请求对象
     * @return JWT token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}