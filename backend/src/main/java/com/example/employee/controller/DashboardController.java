package com.example.employee.controller;

import com.example.employee.common.Result;
import com.example.employee.dto.DashboardOverviewDTO;
import com.example.employee.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<DashboardOverviewDTO> getOverview(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) Long departmentId
    ) {
        return Result.success(dashboardService.getOverview(startDate, endDate, departmentId));
    }
}
