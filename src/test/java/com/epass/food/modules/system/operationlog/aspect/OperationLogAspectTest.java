package com.epass.food.modules.system.operationlog.aspect;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.Result;
import com.epass.food.config.security.LoginUser;
import com.epass.food.modules.system.operationlog.annotation.OperationLog;
import com.epass.food.modules.system.operationlog.entity.SysOperationLog;
import com.epass.food.modules.system.operationlog.service.SysOperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OperationLogAspectTest {

    @Mock
    private SysOperationLogService sysOperationLogService;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRecordSuccessfulOperation() {
        prepareRequestContext("POST", "/system/user", "trace-1");
        TestTarget proxy = createProxy();

        proxy.createUser();

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(sysOperationLogService).recordLog(captor.capture());
        SysOperationLog log = captor.getValue();
        assertThat(log.getModule()).isEqualTo("SYSTEM_USER");
        assertThat(log.getAction()).isEqualTo("CREATE");
        assertThat(log.getMethod()).isEqualTo("POST");
        assertThat(log.getPath()).isEqualTo("/system/user");
        assertThat(log.getRequestId()).isEqualTo("trace-1");
        assertThat(log.getUserId()).isEqualTo(9L);
        assertThat(log.getUsername()).isEqualTo("tester");
        assertThat(log.getSuccess()).isEqualTo(1);
        assertThat(log.getCostMs()).isNotNull();
    }

    @Test
    void shouldRecordFailedOperation() {
        prepareRequestContext("PUT", "/food/order/refund", "trace-2");
        TestTarget proxy = createProxy();

        assertThatThrownBy(proxy::refundOrder)
                .isInstanceOf(BusinessException.class);

        ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
        verify(sysOperationLogService).recordLog(captor.capture());
        SysOperationLog log = captor.getValue();
        assertThat(log.getModule()).isEqualTo("FOOD_ORDER");
        assertThat(log.getAction()).isEqualTo("REFUND");
        assertThat(log.getMethod()).isEqualTo("PUT");
        assertThat(log.getPath()).isEqualTo("/food/order/refund");
        assertThat(log.getSuccess()).isEqualTo(0);
        assertThat(log.getErrorMessage()).contains("refund failed");
    }

    private TestTarget createProxy() {
        OperationLogAspect aspect = new OperationLogAspect(sysOperationLogService);
        AspectJProxyFactory factory = new AspectJProxyFactory(new TestTarget());
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    private void prepareRequestContext(String method, String path, String requestId) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addHeader("X-Request-Id", requestId);
        request.setRemoteAddr("127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        LoginUser loginUser = new LoginUser(9L, "tester", "Tester");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of())
        );
    }

    static class TestTarget {

        @OperationLog(module = "SYSTEM_USER", action = "CREATE")
        public Result<Void> createUser() {
            return Result.success();
        }

        @OperationLog(module = "FOOD_ORDER", action = "REFUND")
        public Result<Void> refundOrder() {
            throw new BusinessException(400, "refund failed");
        }
    }
}
