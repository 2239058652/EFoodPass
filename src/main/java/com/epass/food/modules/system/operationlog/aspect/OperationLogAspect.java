package com.epass.food.modules.system.operationlog.aspect;

import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.system.operationlog.annotation.OperationLog;
import com.epass.food.modules.system.operationlog.entity.SysOperationLog;
import com.epass.food.modules.system.operationlog.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final SysOperationLogService sysOperationLogService;

    public OperationLogAspect(SysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperationLog logRecord = buildBaseRecord(operationLog);

        try {
            Object result = joinPoint.proceed();
            applyResult(logRecord, result);
            return result;
        } catch (Throwable ex) {
            logRecord.setSuccess(0);
            logRecord.setErrorMessage(truncate(resolveErrorMessage(ex), 500));
            throw ex;
        } finally {
            int costMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - start);
            logRecord.setCostMs(costMs);
            if (logRecord.getOperateTime() == null) {
                logRecord.setOperateTime(LocalDateTime.now());
            }
            saveQuietly(logRecord);
        }
    }

    private SysOperationLog buildBaseRecord(OperationLog operationLog) {
        SysOperationLog operationLogEntity = new SysOperationLog();
        operationLogEntity.setRequestId(resolveRequestId());
        operationLogEntity.setModule(operationLog.module());
        operationLogEntity.setAction(operationLog.action());
        operationLogEntity.setSuccess(1);
        operationLogEntity.setOperateTime(LocalDateTime.now());

        ServletRequestAttributes attributes = getServletRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLogEntity.setMethod(request.getMethod());
            operationLogEntity.setPath(request.getRequestURI());
            operationLogEntity.setRequestIp(resolveClientIp(request));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            operationLogEntity.setUserId(loginUser.getUserId());
            operationLogEntity.setUsername(loginUser.getUsername());
        }
        return operationLogEntity;
    }

    private void applyResult(SysOperationLog logRecord, Object result) {
        if (result instanceof Result<?> response && response.getCode() != null && response.getCode() != 200) {
            logRecord.setSuccess(0);
            logRecord.setErrorMessage(truncate(response.getMessage(), 500));
        }
    }

    private void saveQuietly(SysOperationLog operationLog) {
        try {
            sysOperationLogService.recordLog(operationLog);
        } catch (Exception e) {
            log.warn("Failed to record operation log: {}", e.getMessage());
        }
    }

    private ServletRequestAttributes getServletRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes;
        }
        return null;
    }

    private String resolveRequestId() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        if (attributes != null) {
            String requestId = attributes.getRequest().getHeader("X-Request-Id");
            if (requestId != null && !requestId.isBlank()) {
                return truncate(requestId.trim(), 64);
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp.trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    private String resolveErrorMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        if (current.getMessage() != null && !current.getMessage().isBlank()) {
            return current.getMessage();
        }
        return current.getClass().getSimpleName();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
