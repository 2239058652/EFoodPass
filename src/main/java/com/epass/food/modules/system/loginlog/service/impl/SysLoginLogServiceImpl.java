package com.epass.food.modules.system.loginlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.loginlog.dto.LoginLogListQuery;
import com.epass.food.modules.system.loginlog.dto.LoginLogListResponse;
import com.epass.food.modules.system.loginlog.entity.SysLoginLog;
import com.epass.food.modules.system.loginlog.mapper.SysLoginLogMapper;
import com.epass.food.modules.system.loginlog.service.SysLoginLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements SysLoginLogService {

    @Override
    public PageResult<LoginLogListResponse> listLogs(LoginLogListQuery query) {
        if (query == null) {
            query = new LoginLogListQuery();
        }

        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getUsername())) {
            queryWrapper.like(SysLoginLog::getUsername, query.getUsername().trim());
        }
        if (query.getSuccess() != null) {
            queryWrapper.eq(SysLoginLog::getSuccess, query.getSuccess());
        }
        if (query.getLoginTimeStart() != null) {
            queryWrapper.ge(SysLoginLog::getLoginTime, query.getLoginTimeStart());
        }
        if (query.getLoginTimeEnd() != null) {
            queryWrapper.le(SysLoginLog::getLoginTime, query.getLoginTimeEnd());
        }
        queryWrapper.orderByDesc(SysLoginLog::getId);

        Page<SysLoginLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysLoginLog> loginLogPage = this.page(page, queryWrapper);

        List<LoginLogListResponse> responseList = new ArrayList<>();
        for (SysLoginLog loginLog : loginLogPage.getRecords()) {
            LoginLogListResponse response = new LoginLogListResponse();
            response.setId(loginLog.getId());
            response.setUserId(loginLog.getUserId());
            response.setUsername(loginLog.getUsername());
            response.setRequestIp(loginLog.getRequestIp());
            response.setSuccess(loginLog.getSuccess());
            response.setMessage(loginLog.getMessage());
            response.setLoginTime(loginLog.getLoginTime());
            responseList.add(response);
        }

        PageResult<LoginLogListResponse> result = new PageResult<>();
        result.setTotal(loginLogPage.getTotal());
        result.setPageNum(loginLogPage.getCurrent());
        result.setPageSize(loginLogPage.getSize());
        result.setRecords(responseList);
        return result;
    }

    @Override
    public void recordLogin(Long userId, String username, String requestIp, Integer success, String message) {
        SysLoginLog loginLog = new SysLoginLog();
        loginLog.setUserId(userId);
        loginLog.setUsername(username);
        loginLog.setRequestIp(requestIp);
        loginLog.setSuccess(success);
        loginLog.setMessage(message);
        loginLog.setLoginTime(LocalDateTime.now());
        this.save(loginLog);
    }
}
