package com.epass.food.modules.admin.controller;

import com.epass.food.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminTestController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public Result<String> dashboard() {
        return Result.success("admin dashboard");
    }

    @PreAuthorize("hasAuthority('admin:dashboard')")
    @GetMapping("/dashboard2")
    public Result<String> dashboard2() {
        return Result.success("admin dashboard by authority");
    }
}