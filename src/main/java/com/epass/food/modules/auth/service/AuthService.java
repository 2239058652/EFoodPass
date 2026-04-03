package com.epass.food.modules.auth.service;

import com.epass.food.modules.auth.dto.CurrentSessionResponse;
import com.epass.food.modules.auth.dto.CurrentUserChangePasswordRequest;
import com.epass.food.modules.auth.dto.CurrentUserResponse;
import com.epass.food.modules.auth.dto.CurrentUserUpdateRequest;
import com.epass.food.modules.auth.dto.LoginRequest;
import com.epass.food.modules.auth.dto.LoginResponse;
import com.epass.food.modules.auth.dto.UserSessionResponse;

import java.util.List;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(Long userId);

    void logoutCurrentSession(Long userId, String token);

    LoginResponse refreshToken(Long userId, String token);

    CurrentUserResponse getCurrentUser(Long userId);

    CurrentUserResponse updateCurrentUser(Long userId, CurrentUserUpdateRequest request);

    void changeCurrentUserPassword(Long userId, CurrentUserChangePasswordRequest request);

    CurrentSessionResponse getCurrentSession(Long userId, String token);

    List<UserSessionResponse> listSessions(Long userId, String token);

    void offlineSession(Long userId, String sessionId);
}
