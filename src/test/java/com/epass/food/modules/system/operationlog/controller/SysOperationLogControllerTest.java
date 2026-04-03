package com.epass.food.modules.system.operationlog.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.operationlog.dto.OperationLogListResponse;
import com.epass.food.modules.system.operationlog.service.SysOperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysOperationLogControllerTest {

    @Mock
    private SysOperationLogService sysOperationLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        SysOperationLogController controller = new SysOperationLogController(sysOperationLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listShouldReturnServiceResult() throws Exception {
        OperationLogListResponse record = new OperationLogListResponse();
        record.setId(1L);
        record.setModule("SYSTEM_USER");
        record.setAction("CREATE");

        PageResult<OperationLogListResponse> pageResult = new PageResult<>();
        pageResult.setTotal(1L);
        pageResult.setPageNum(1L);
        pageResult.setPageSize(10L);
        pageResult.setRecords(List.of(record));
        when(sysOperationLogService.listLogs(org.mockito.ArgumentMatchers.any())).thenReturn(pageResult);

        mockMvc.perform(get("/system/operation-log/list")
                        .param("module", "SYSTEM_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].module").value("SYSTEM_USER"))
                .andExpect(jsonPath("$.data.records[0].action").value("CREATE"));

        verify(sysOperationLogService).listLogs(org.mockito.ArgumentMatchers.any());
    }
}
