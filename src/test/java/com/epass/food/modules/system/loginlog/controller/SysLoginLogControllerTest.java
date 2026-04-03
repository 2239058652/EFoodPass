package com.epass.food.modules.system.loginlog.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.loginlog.dto.LoginLogListResponse;
import com.epass.food.modules.system.loginlog.service.SysLoginLogService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SysLoginLogControllerTest {

    @Mock
    private SysLoginLogService sysLoginLogService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        SysLoginLogController controller = new SysLoginLogController(sysLoginLogService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listShouldReturnServiceResult() throws Exception {
        LoginLogListResponse record = new LoginLogListResponse();
        record.setId(1L);
        record.setUsername("admin");
        record.setSuccess(1);

        PageResult<LoginLogListResponse> pageResult = new PageResult<>();
        pageResult.setTotal(1L);
        pageResult.setPageNum(1L);
        pageResult.setPageSize(10L);
        pageResult.setRecords(List.of(record));
        when(sysLoginLogService.listLogs(any())).thenReturn(pageResult);

        mockMvc.perform(get("/system/login-log/list").param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"))
                .andExpect(jsonPath("$.data.records[0].success").value(1));

        verify(sysLoginLogService).listLogs(any());
    }
}
