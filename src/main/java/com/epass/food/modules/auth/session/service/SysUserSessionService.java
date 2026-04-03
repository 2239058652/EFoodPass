package com.epass.food.modules.auth.session.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.modules.auth.session.entity.SysUserSession;

import java.time.LocalDateTime;
import java.util.List;

public interface SysUserSessionService extends IService<SysUserSession> {

    SysUserSession createSession(Long userId,
                                 Integer tokenVersion,
                                 String requestIp,
                                 String userAgent,
                                 LocalDateTime expireTime);

    SysUserSession getValidSession(Long userId, String sessionId, Integer tokenVersion);

    List<SysUserSession> listActiveSessions(Long userId, Integer tokenVersion);

    boolean removeSession(Long userId, String sessionId, Integer tokenVersion);

    void removeUserSessions(Long userId);

    void touchSession(String sessionId, LocalDateTime accessTime);
}
