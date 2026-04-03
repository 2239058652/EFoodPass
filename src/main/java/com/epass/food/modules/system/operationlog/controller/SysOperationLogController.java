package com.epass.food.modules.system.operationlog.controller;

import com.epass.food.common.page.PageResult;
import com.epass.food.common.result.Result;
import com.epass.food.modules.system.operationlog.dto.OperationLogListQuery;
import com.epass.food.modules.system.operationlog.dto.OperationLogListResponse;
import com.epass.food.modules.system.operationlog.service.SysOperationLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/operation-log")
public class SysOperationLogController {

    private final SysOperationLogService sysOperationLogService;

    public SysOperationLogController(SysOperationLogService sysOperationLogService) {
        this.sysOperationLogService = sysOperationLogService;
    }

    @PreAuthorize("hasAuthority('system:operation-log:list')")
    @GetMapping("/list")
    public Result<PageResult<OperationLogListResponse>> list(OperationLogListQuery query) {
        return Result.success(sysOperationLogService.listLogs(query));
    }
}
