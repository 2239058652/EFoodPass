package com.epass.food.modules.system.operationlog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.epass.food.common.page.PageResult;
import com.epass.food.modules.system.operationlog.dto.OperationLogListQuery;
import com.epass.food.modules.system.operationlog.dto.OperationLogListResponse;
import com.epass.food.modules.system.operationlog.entity.SysOperationLog;
import com.epass.food.modules.system.operationlog.mapper.SysOperationLogMapper;
import com.epass.food.modules.system.operationlog.service.SysOperationLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class SysOperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements SysOperationLogService {

    @Override
    public PageResult<OperationLogListResponse> listLogs(OperationLogListQuery query) {
        if (query == null) {
            query = new OperationLogListQuery();
        }

        LambdaQueryWrapper<SysOperationLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getModule())) {
            queryWrapper.like(SysOperationLog::getModule, query.getModule().trim());
        }
        if (StringUtils.hasText(query.getAction())) {
            queryWrapper.like(SysOperationLog::getAction, query.getAction().trim());
        }
        if (StringUtils.hasText(query.getUsername())) {
            queryWrapper.like(SysOperationLog::getUsername, query.getUsername().trim());
        }
        if (query.getSuccess() != null) {
            queryWrapper.eq(SysOperationLog::getSuccess, query.getSuccess());
        }
        if (query.getOperateTimeStart() != null) {
            queryWrapper.ge(SysOperationLog::getOperateTime, query.getOperateTimeStart());
        }
        if (query.getOperateTimeEnd() != null) {
            queryWrapper.le(SysOperationLog::getOperateTime, query.getOperateTimeEnd());
        }
        queryWrapper.orderByDesc(SysOperationLog::getId);

        Page<SysOperationLog> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<SysOperationLog> logPage = this.page(page, queryWrapper);

        List<OperationLogListResponse> responseList = new ArrayList<>();
        for (SysOperationLog operationLog : logPage.getRecords()) {
            OperationLogListResponse response = new OperationLogListResponse();
            response.setId(operationLog.getId());
            response.setRequestId(operationLog.getRequestId());
            response.setUserId(operationLog.getUserId());
            response.setUsername(operationLog.getUsername());
            response.setModule(operationLog.getModule());
            response.setAction(operationLog.getAction());
            response.setMethod(operationLog.getMethod());
            response.setPath(operationLog.getPath());
            response.setRequestIp(operationLog.getRequestIp());
            response.setSuccess(operationLog.getSuccess());
            response.setErrorMessage(operationLog.getErrorMessage());
            response.setCostMs(operationLog.getCostMs());
            response.setOperateTime(operationLog.getOperateTime());
            responseList.add(response);
        }

        PageResult<OperationLogListResponse> result = new PageResult<>();
        result.setTotal(logPage.getTotal());
        result.setPageNum(logPage.getCurrent());
        result.setPageSize(logPage.getSize());
        result.setRecords(responseList);
        return result;
    }

    @Override
    public void recordLog(SysOperationLog operationLog) {
        this.save(operationLog);
    }
}
