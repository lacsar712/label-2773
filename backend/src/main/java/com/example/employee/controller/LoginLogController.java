package com.example.employee.controller;

import com.example.employee.common.Result;
import com.example.employee.context.UserContext;
import com.example.employee.dto.LoginLogQueryDTO;
import com.example.employee.dto.PageResultDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.SysLoginLog;
import com.example.employee.service.SysLoginLogService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/login-logs")
@Tag(name = "登录日志")
@CrossOrigin(origins = "*")
public class LoginLogController {

    @Autowired
    private SysLoginLogService sysLoginLogService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<PageResultDTO<SysLoginLog>> getLoginLogPage(@ModelAttribute LoginLogQueryDTO query) {
        UserInfoDTO currentUser = UserContext.getCurrentUser();
        return Result.success(sysLoginLogService.getLoginLogPage(query, currentUser.getUserId(), currentUser.getRoleCode()));
    }
}
