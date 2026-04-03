package com.epass.food.modules.system.operationlog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.operationlog.dto.OperationLogListQuery;
import com.epass.food.modules.system.operationlog.dto.OperationLogListResponse;
import com.epass.food.modules.system.operationlog.entity.SysOperationLog;

public interface SysOperationLogService extends IService<SysOperationLog> {

    PageResult<OperationLogListResponse> listLogs(OperationLogListQuery query);

    void recordLog(SysOperationLog operationLog);
}
