package com.epass.food.modules.system.loginlog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.epass.food.modules.system.loginlog.dto.LoginLogListQuery;
import com.epass.food.modules.system.loginlog.dto.LoginLogListResponse;
import com.epass.food.modules.system.loginlog.entity.SysLoginLog;
import com.epass.food.modules.system.loginlog.mapper.SysLoginLogMapper;
import com.epass.food.modules.system.loginlog.service.impl.SysLoginLogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SysLoginLogServiceImplTest {

    @Mock
    private SysLoginLogMapper sysLoginLogMapper;

    private SysLoginLogServiceImpl sysLoginLogService;

    @BeforeEach
    void setUp() {
        sysLoginLogService = new SysLoginLogServiceImpl();
        ReflectionTestUtils.setField(sysLoginLogService, "baseMapper", sysLoginLogMapper);
    }

    @Test
    void listLogsShouldMapPagedRecords() {
        LocalDateTime loginTime = LocalDateTime.of(2026, 4, 3, 16, 0);
        SysLoginLog record = new SysLoginLog();
        record.setId(1L);
        record.setUsername("admin");
        record.setSuccess(1);
        record.setLoginTime(loginTime);

        doAnswer(invocation -> {
            Page<SysLoginLog> page = invocation.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1L);
            return page;
        }).when(sysLoginLogMapper).selectPage(any(Page.class), any());

        LoginLogListQuery query = new LoginLogListQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setUsername("admin");

        var result = sysLoginLogService.listLogs(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        LoginLogListResponse response = result.getRecords().get(0);
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getSuccess()).isEqualTo(1);
        assertThat(response.getLoginTime()).isEqualTo(loginTime);
        verify(sysLoginLogMapper).selectPage(any(Page.class), any());
    }
}
