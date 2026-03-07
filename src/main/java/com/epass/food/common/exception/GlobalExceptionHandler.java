package com.epass.food.common.exception;

import com.epass.food.common.result.Result;
import com.epass.food.common.result.ResultCode;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * 添加 @RestControllerAdvice 注解，表示该类为全局异常处理类
 * 添加 @ExceptionHandler 注解，表示该方法处理指定的异常
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return new Result<>(e.getCode(), e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }
}