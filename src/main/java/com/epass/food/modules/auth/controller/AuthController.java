package com.epass.food.modules.auth.controller;

import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.auth.dto.CurrentSessionResponse;
import com.epass.food.modules.auth.dto.CurrentUserChangePasswordRequest;
import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.CurrentUserUpdateRequest;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.dto.UserSessionResponse;
import com.epass.food.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Authentication", description = "Login, logout, token refresh and current session APIs")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Authenticate with username and password")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "Logout all", description = "Invalidate all sessions of the current user")
    @PostMapping("/logout")
    public Result<Void> logout(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        authService.logout(loginUser.getUserId());
        return Result.success();
    }

    @Operation(summary = "Refresh token", description = "Issue a new token for the current authenticated session")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(Authentication authentication,
                                         @RequestHeader("Authorization") String authorization) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(authService.refreshToken(loginUser.getUserId(), extractToken(authorization)));
    }

    @Operation(summary = "Current user", description = "Return current user detail from authenticated context")
    @GetMapping("/me")
    public Result<CurrentUserResponse> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(authService.getCurrentUser(loginUser.getUserId()));
    }

    @Operation(summary = "Update current user", description = "Update nickname and phone of the current user")
    @PutMapping("/profile")
    public Result<CurrentUserResponse> updateProfile(Authentication authentication,
                                                     @Valid @RequestBody CurrentUserUpdateRequest request) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(authService.updateCurrentUser(loginUser.getUserId(), request));
    }

    @Operation(summary = "Change password", description = "Change password of the current user and invalidate all sessions")
    @PutMapping("/password")
    public Result<Void> changePassword(Authentication authentication,
                                       @Valid @RequestBody CurrentUserChangePasswordRequest request) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        authService.changeCurrentUserPassword(loginUser.getUserId(), request);
        return Result.success();
    }

    @Operation(summary = "Current session", description = "Return details of the current authenticated session")
    @GetMapping("/session/current")
    public Result<CurrentSessionResponse> currentSession(Authentication authentication,
                                                         @RequestHeader("Authorization") String authorization) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(authService.getCurrentSession(loginUser.getUserId(), extractToken(authorization)));
    }

    @Operation(summary = "List sessions", description = "Return active sessions of the current user")
    @GetMapping("/session/list")
    public Result<List<UserSessionResponse>> listSessions(Authentication authentication,
                                                          @RequestHeader("Authorization") String authorization) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(authService.listSessions(loginUser.getUserId(), extractToken(authorization)));
    }

    @Operation(summary = "Logout current session", description = "Invalidate only the current device session")
    @DeleteMapping("/session/current")
    public Result<Void> logoutCurrentSession(Authentication authentication,
                                             @RequestHeader("Authorization") String authorization) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        authService.logoutCurrentSession(loginUser.getUserId(), extractToken(authorization));
        return Result.success();
    }

    @Operation(summary = "Offline session", description = "Invalidate a specified session of the current user")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> offlineSession(Authentication authentication, @PathVariable String sessionId) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        authService.offlineSession(loginUser.getUserId(), sessionId);
        return Result.success();
    }

    private String extractToken(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : authorization;
    }
}
