package com.epass.food.modules.system.loginlog.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.system.loginlog.dto.LoginLogListQuery;
import com.epass.food.modules.system.loginlog.dto.LoginLogListResponse;
import com.epass.food.modules.system.loginlog.service.SysLoginLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/login-log")
public class SysLoginLogController {

    private final SysLoginLogService sysLoginLogService;

    public SysLoginLogController(SysLoginLogService sysLoginLogService) {
        this.sysLoginLogService = sysLoginLogService;
    }

    @PreAuthorize("hasAuthority('system:login-log:list')")
    @GetMapping("/list")
    public Result<PageResult<LoginLogListResponse>> list(LoginLogListQuery query) {
        return Result.success(sysLoginLogService.listLogs(query));
    }
}
