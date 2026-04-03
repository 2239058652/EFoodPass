package com.epass.food.modules.system.loginlog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.loginlog.dto.LoginLogListQuery;
import com.epass.food.modules.system.loginlog.dto.LoginLogListResponse;
import com.epass.food.modules.system.loginlog.entity.SysLoginLog;

public interface SysLoginLogService extends IService<SysLoginLog> {

    PageResult<LoginLogListResponse> listLogs(LoginLogListQuery query);

    void recordLogin(Long userId, String username, String requestIp, Integer success, String message);
}
