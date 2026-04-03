package com.epass.food.modules.auth.session.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.modules.auth.session.entity.SysUserSession;
import com.epass.food.modules.auth.session.mapper.SysUserSessionMapper;
import com.epass.food.modules.auth.session.service.SysUserSessionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SysUserSessionServiceImpl extends ServiceImpl<SysUserSessionMapper, SysUserSession> implements SysUserSessionService {

    @Override
    public SysUserSession createSession(Long userId,
                                        Integer tokenVersion,
                                        String requestIp,
                                        String userAgent,
                                        LocalDateTime expireTime) {
        SysUserSession session = new SysUserSession();
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setTokenVersion(tokenVersion);
        session.setRequestIp(requestIp);
        session.setUserAgent(userAgent);
        session.setLoginTime(LocalDateTime.now());
        session.setLastAccessTime(session.getLoginTime());
        session.setExpireTime(expireTime);
        this.save(session);
        return session;
    }

    @Override
    public SysUserSession getValidSession(Long userId, String sessionId, Integer tokenVersion) {
        cleanupExpiredSessions(userId, tokenVersion);
        if (!StringUtils.hasText(sessionId)) {
            return null;
        }

        return this.getOne(new LambdaQueryWrapper<SysUserSession>()
                .eq(SysUserSession::getUserId, userId)
                .eq(SysUserSession::getSessionId, sessionId.trim())
                .eq(SysUserSession::getTokenVersion, tokenVersion)
                .last("LIMIT 1"));
    }

    @Override
    public List<SysUserSession> listActiveSessions(Long userId, Integer tokenVersion) {
        cleanupExpiredSessions(userId, tokenVersion);
        return this.list(new LambdaQueryWrapper<SysUserSession>()
                .eq(SysUserSession::getUserId, userId)
                .eq(SysUserSession::getTokenVersion, tokenVersion)
                .ge(SysUserSession::getExpireTime, LocalDateTime.now())
                .orderByDesc(SysUserSession::getLastAccessTime)
                .orderByDesc(SysUserSession::getLoginTime));
    }

    @Override
    public boolean removeSession(Long userId, String sessionId, Integer tokenVersion) {
        if (!StringUtils.hasText(sessionId)) {
            return false;
        }

        LambdaQueryWrapper<SysUserSession> queryWrapper = new LambdaQueryWrapper<SysUserSession>()
                .eq(SysUserSession::getUserId, userId)
                .eq(SysUserSession::getSessionId, sessionId.trim());
        if (tokenVersion != null) {
            queryWrapper.eq(SysUserSession::getTokenVersion, tokenVersion);
        }
        return this.remove(queryWrapper);
    }

    @Override
    public void removeUserSessions(Long userId) {
        this.remove(new LambdaQueryWrapper<SysUserSession>()
                .eq(SysUserSession::getUserId, userId));
    }

    @Override
    public void touchSession(String sessionId, LocalDateTime accessTime) {
        if (!StringUtils.hasText(sessionId) || accessTime == null) {
            return;
        }

        SysUserSession update = new SysUserSession();
        update.setLastAccessTime(accessTime);
        this.update(update, new LambdaQueryWrapper<SysUserSession>()
                .eq(SysUserSession::getSessionId, sessionId.trim()));
    }

    private void cleanupExpiredSessions(Long userId, Integer tokenVersion) {
        this.remove(new LambdaQueryWrapper<SysUserSession>()
                .eq(SysUserSession::getUserId, userId)
                .eq(SysUserSession::getTokenVersion, tokenVersion)
                .lt(SysUserSession::getExpireTime, LocalDateTime.now()));
    }
}
