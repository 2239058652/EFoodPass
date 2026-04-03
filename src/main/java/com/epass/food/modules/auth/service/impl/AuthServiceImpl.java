package com.epass.food.modules.auth.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.BizErrorCode;
import com.epass.food.config.security.JwtTokenProvider;
import com.epass.food.modules.auth.dto.CurrentSessionResponse;
import com.epass.food.modules.auth.dto.CurrentUserChangePasswordRequest;
import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.CurrentUserUpdateRequest;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.dto.UserSessionResponse;
import com.epass.food.modules.auth.service.AuthService;
import com.epass.food.modules.auth.session.entity.SysUserSession;
import com.epass.food.modules.auth.session.service.SysUserSessionService;
import com.epass.food.modules.system.loginlog.service.SysLoginLogService;
import com.epass.food.modules.system.permission.service.SysPermissionService;
import com.epass.food.modules.system.role.entity.SysRole;
import com.epass.food.modules.system.role.service.SysRoleService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.service.SysUserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final SysRoleService sysRoleService;
    private final SysPermissionService sysPermissionService;
    private final SysLoginLogService sysLoginLogService;
    private final SysUserSessionService sysUserSessionService;

    public AuthServiceImpl(SysUserService sysUserService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           SysRoleService sysRoleService,
                           SysPermissionService sysPermissionService,
                           SysLoginLogService sysLoginLogService,
                           SysUserSessionService sysUserSessionService) {
        this.sysUserService = sysUserService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sysRoleService = sysRoleService;
        this.sysPermissionService = sysPermissionService;
        this.sysLoginLogService = sysLoginLogService;
        this.sysUserSessionService = sysUserSessionService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        SysUser user = sysUserService.getByUsername(username);

        if (user == null) {
            recordLogin(null, username, 0, "USERNAME_OR_PASSWORD_INVALID");
            throw new BusinessException(BizErrorCode.AUTH_USERNAME_OR_PASSWORD_INVALID, "username or password is invalid");
        }

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            recordLogin(user, user.getUsername(), 0, "USER_DISABLED");
            throw new BusinessException(BizErrorCode.AUTH_USER_DISABLED, "user is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordLogin(user, user.getUsername(), 0, "USERNAME_OR_PASSWORD_INVALID");
            throw new BusinessException(BizErrorCode.AUTH_USERNAME_OR_PASSWORD_INVALID, "username or password is invalid");
        }

        user.setLastLoginAt(LocalDateTime.now());
        sysUserService.updateById(user);
        recordLogin(user, user.getUsername(), 1, "LOGIN_SUCCESS");

        SysUserSession session = createSession(user);
        String token = jwtTokenProvider.createToken(
                user.getId(),
                user.getUsername(),
                normalizeTokenVersion(user.getTokenVersion()),
                session.getSessionId()
        );
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public void logout(Long userId) {
        SysUser user = getExistingUser(userId);
        int oldVersion = normalizeTokenVersion(user.getTokenVersion());
        user.setTokenVersion(oldVersion + 1);
        sysUserService.updateById(user);
        sysUserSessionService.removeUserSessions(userId);
    }

    @Override
    public void logoutCurrentSession(Long userId, String token) {
        SysUser user = getExistingUser(userId);
        SysUserSession session = requireSession(user, token);
        sysUserSessionService.removeSession(userId, session.getSessionId(), normalizeTokenVersion(user.getTokenVersion()));
    }

    @Override
    public LoginResponse refreshToken(Long userId, String token) {
        SysUser user = getExistingUser(userId);
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(BizErrorCode.AUTH_USER_DISABLED, "user is disabled");
        }

        SysUserSession currentSession = requireSession(user, token);
        SysUserSession newSession = createSession(user);
        sysUserSessionService.removeSession(userId, currentSession.getSessionId(), normalizeTokenVersion(user.getTokenVersion()));

        String newToken = jwtTokenProvider.createToken(
                user.getId(),
                user.getUsername(),
                normalizeTokenVersion(user.getTokenVersion()),
                newSession.getSessionId()
        );
        return new LoginResponse(newToken, user.getId(), user.getUsername(), user.getNickname());
    }

    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {
        SysUser user = getExistingUser(userId);
        return buildCurrentUserResponse(user);
    }

    @Override
    public CurrentUserResponse updateCurrentUser(Long userId, CurrentUserUpdateRequest request) {
        SysUser user = getExistingUser(userId);
        String normalizedPhone = normalizePhone(request.getPhone());
        if (StringUtils.hasText(normalizedPhone)) {
            SysUser existUser = sysUserService.getByPhone(normalizedPhone);
            if (existUser != null && !existUser.getId().equals(userId)) {
                throw new BusinessException(BizErrorCode.USER_PHONE_EXISTS, "phone already exists");
            }
        }

        user.setNickname(request.getNickname().trim());
        user.setPhone(normalizedPhone);
        sysUserService.updateById(user);
        return buildCurrentUserResponse(user);
    }

    @Override
    public void changeCurrentUserPassword(Long userId, CurrentUserChangePasswordRequest request) {
        SysUser user = getExistingUser(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(BizErrorCode.USER_OLD_PASSWORD_INVALID, "old password is invalid");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(normalizeTokenVersion(user.getTokenVersion()) + 1);
        sysUserService.updateById(user);
        sysUserSessionService.removeUserSessions(userId);
    }

    @Override
    public CurrentSessionResponse getCurrentSession(Long userId, String token) {
        SysUser user = getExistingUser(userId);
        Claims claims = parseTokenForUser(token, userId);
        String sessionId = extractSessionId(claims);
        SysUserSession session = requireActiveSession(user.getId(), normalizeTokenVersion(user.getTokenVersion()), sessionId);

        CurrentSessionResponse response = new CurrentSessionResponse();
        response.setUserId(user.getId());
        response.setSessionId(session.getSessionId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setTokenVersion(normalizeTokenVersion(user.getTokenVersion()));
        response.setRequestIp(session.getRequestIp());
        response.setUserAgent(session.getUserAgent());
        response.setLoginTime(session.getLoginTime());
        response.setLastAccessTime(session.getLastAccessTime());
        response.setIssuedAt(jwtTokenProvider.toLocalDateTime(claims.getIssuedAt()));
        response.setExpiresAt(session.getExpireTime());
        response.setRemainingSeconds(Math.max(
                0,
                session.getExpireTime().atZone(ZoneId.systemDefault()).toEpochSecond() - Instant.now().getEpochSecond()
        ));
        return response;
    }

    @Override
    public List<UserSessionResponse> listSessions(Long userId, String token) {
        SysUser user = getExistingUser(userId);
        Claims claims = parseTokenForUser(token, userId);
        String currentSessionId = extractSessionId(claims);

        return sysUserSessionService.listActiveSessions(userId, normalizeTokenVersion(user.getTokenVersion()))
                .stream()
                .map(session -> {
                    UserSessionResponse response = new UserSessionResponse();
                    response.setSessionId(session.getSessionId());
                    response.setRequestIp(session.getRequestIp());
                    response.setUserAgent(session.getUserAgent());
                    response.setLoginTime(session.getLoginTime());
                    response.setLastAccessTime(session.getLastAccessTime());
                    response.setExpiresAt(session.getExpireTime());
                    response.setCurrent(session.getSessionId().equals(currentSessionId));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void offlineSession(Long userId, String sessionId) {
        SysUser user = getExistingUser(userId);
        boolean removed = sysUserSessionService.removeSession(
                userId,
                sessionId,
                normalizeTokenVersion(user.getTokenVersion())
        );
        if (!removed) {
            throw new BusinessException(BizErrorCode.USER_SESSION_NOT_FOUND, "session not found");
        }
    }

    private CurrentUserResponse buildCurrentUserResponse(SysUser user) {
        Long userId = user.getId();
        List<SysRole> roleList = sysRoleService.getRolesByUserId(userId);
        List<String> permissionCodes = sysPermissionService.getPermissionCodesByUserId(userId);
        List<String> roleCodes = roleList.stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());

        CurrentUserResponse response = new CurrentUserResponse();
        response.setUserId(userId);
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setPhone(user.getPhone());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setRoleCodes(roleCodes);
        response.setPermissionCodes(permissionCodes);
        return response;
    }

    private SysUser getExistingUser(Long userId) {
        SysUser user = sysUserService.getById(userId);
        if (user == null) {
            throw new BusinessException(BizErrorCode.USER_NOT_FOUND, "user not found");
        }
        return user;
    }

    private SysUserSession createSession(SysUser user) {
        int tokenVersion = normalizeTokenVersion(user.getTokenVersion());
        LocalDateTime expireTime = LocalDateTime.now().plusSeconds(jwtTokenProvider.getExpireSeconds());
        return sysUserSessionService.createSession(
                user.getId(),
                tokenVersion,
                resolveClientIp(),
                resolveUserAgent(),
                expireTime
        );
    }

    private Claims parseTokenForUser(String token, Long userId) {
        Claims claims = jwtTokenProvider.parseToken(token);
        if (!String.valueOf(userId).equals(claims.getSubject())) {
            throw new BusinessException(BizErrorCode.USER_SESSION_NOT_FOUND, "session not found");
        }
        return claims;
    }

    private String extractSessionId(Claims claims) {
        String sessionId = claims.get("sessionId", String.class);
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException(BizErrorCode.USER_SESSION_NOT_FOUND, "session not found");
        }
        return sessionId.trim();
    }

    private SysUserSession requireSession(SysUser user, String token) {
        Claims claims = parseTokenForUser(token, user.getId());
        return requireActiveSession(
                user.getId(),
                normalizeTokenVersion(user.getTokenVersion()),
                extractSessionId(claims)
        );
    }

    private SysUserSession requireActiveSession(Long userId, Integer tokenVersion, String sessionId) {
        SysUserSession session = sysUserSessionService.getValidSession(userId, sessionId, tokenVersion);
        if (session == null) {
            throw new BusinessException(BizErrorCode.USER_SESSION_NOT_FOUND, "session not found");
        }
        return session;
    }

    private int normalizeTokenVersion(Integer tokenVersion) {
        return tokenVersion == null ? 0 : tokenVersion;
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return phone.trim();
    }

    private String resolveClientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        HttpServletRequest request = servletRequestAttributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveUserAgent() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }

        return servletRequestAttributes.getRequest().getHeader("User-Agent");
    }

    private void recordLogin(SysUser user, String username, Integer success, String message) {
        sysLoginLogService.recordLogin(
                user == null ? null : user.getId(),
                username,
                resolveClientIp(),
                success,
                message
        );
    }
}
