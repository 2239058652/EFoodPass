package com.epass.food.modules.system.operationlog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.epass.food.modules.system.operationlog.dto.OperationLogListQuery;
import com.epass.food.modules.system.operationlog.dto.OperationLogListResponse;
import com.epass.food.modules.system.operationlog.entity.SysOperationLog;
import com.epass.food.modules.system.operationlog.mapper.SysOperationLogMapper;
import com.epass.food.modules.system.operationlog.service.impl.SysOperationLogServiceImpl;
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
class SysOperationLogServiceImplTest {

    @Mock
    private SysOperationLogMapper sysOperationLogMapper;

    private SysOperationLogServiceImpl sysOperationLogService;

    @BeforeEach
    void setUp() {
        sysOperationLogService = new SysOperationLogServiceImpl();
        ReflectionTestUtils.setField(sysOperationLogService, "baseMapper", sysOperationLogMapper);
    }

    @Test
    void listLogsShouldMapPagedRecords() {
        LocalDateTime operateTime = LocalDateTime.of(2026, 4, 3, 15, 0);
        SysOperationLog record = new SysOperationLog();
        record.setId(1L);
        record.setModule("FOOD_ORDER");
        record.setAction("REFUND");
        record.setSuccess(1);
        record.setOperateTime(operateTime);

        doAnswer(invocation -> {
            Page<SysOperationLog> page = invocation.getArgument(0);
            page.setRecords(List.of(record));
            page.setTotal(1L);
            return page;
        }).when(sysOperationLogMapper).selectPage(any(Page.class), any());

        OperationLogListQuery query = new OperationLogListQuery();
        query.setPageNum(1L);
        query.setPageSize(10L);
        query.setModule("FOOD_ORDER");

        var result = sysOperationLogService.listLogs(query);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRecords()).hasSize(1);
        OperationLogListResponse response = result.getRecords().get(0);
        assertThat(response.getModule()).isEqualTo("FOOD_ORDER");
        assertThat(response.getAction()).isEqualTo("REFUND");
        assertThat(response.getSuccess()).isEqualTo(1);
        assertThat(response.getOperateTime()).isEqualTo(operateTime);
        verify(sysOperationLogMapper).selectPage(any(Page.class), any());
    }
}
