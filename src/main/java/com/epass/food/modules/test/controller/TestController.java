package com.epass.food.modules.test.controller;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.common.result.Result;
import com.epass.food.modules.system.permission.service.SysPermissionService;
import com.epass.food.modules.system.user.entity.SysUser;
import com.epass.food.modules.system.user.service.SysUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test")
public class TestController {

    private final SysUserService sysUserService;
    private final SysPermissionService sysPermissionService;

    public TestController(SysUserService sysUserService, SysPermissionService sysPermissionService) {
        this.sysUserService = sysUserService;
        this.sysPermissionService = sysPermissionService;
        System.out.println("TestController---初始化完成");
    }

    @GetMapping("/hello")
    public Result<String> hello() {
        return Result.success("ok");
    }

    // 主动抛业务异常
    @GetMapping("/business-error")
    public Result<Void> businessError() {
        throw new BusinessException(4001, "这是一个业务异常");
    }

    // 模拟系统异常
    @GetMapping("/system-error")
    public Result<Void> systemError() {
        int a = 1 / 0; // 模拟系统异常
        return Result.success();
    }

    @GetMapping("/user")
    public Result<SysUser> getUser() {
        SysUser user = sysUserService.getByUsername("admin");
        return Result.success(user);
    }

    @GetMapping("/permissions")
    public Result<List<String>> permissions() {
        return Result.success(sysPermissionService.getPermissionCodesByUserId(1L));
    }
}