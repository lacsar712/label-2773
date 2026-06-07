package com.example.employee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.employee.common.Result;
import com.example.employee.context.UserContext;
import com.example.employee.dto.LeaveApplicationQueryDTO;
import com.example.employee.dto.LeaveApprovalDTO;
import com.example.employee.dto.LeaveDaysCalculateDTO;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.entity.LeaveApplication;
import com.example.employee.entity.LeaveBalance;
import com.example.employee.service.HolidayService;
import com.example.employee.service.LeaveApplicationService;
import com.example.employee.service.LeaveBalanceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave")
@CrossOrigin(origins = "*")
public class LeaveController {

    @Autowired
    private LeaveApplicationService leaveApplicationService;

    @Autowired
    private LeaveBalanceService leaveBalanceService;

    @Autowired
    private HolidayService holidayService;

    @PostMapping("/calculate-days")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<LeaveDaysCalculateDTO> calculateDays(@RequestBody @Valid LeaveDaysCalculateDTO dto) {
        return Result.success(leaveApplicationService.calculateWorkDays(dto));
    }

    @PostMapping("/draft")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<LeaveApplication> createDraft(@RequestBody @Valid LeaveApplication application) {
        return Result.success(leaveApplicationService.createDraft(application));
    }

    @PostMapping("/submit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<LeaveApplication> submitApplication(@PathVariable Long id) {
        return Result.success(leaveApplicationService.submitApplication(id));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<LeaveApplication> approveApplication(@RequestBody @Valid LeaveApprovalDTO dto) {
        UserInfoDTO currentUser = UserContext.getCurrentUser();
        if (currentUser != null && currentUser.getEmployeeId() != null) {
            dto.setApproverId(currentUser.getEmployeeId());
        }
        return Result.success(leaveApplicationService.approveApplication(dto));
    }

    @PostMapping("/cancel/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<LeaveApplication> cancelApplication(@PathVariable Long id) {
        return Result.success(leaveApplicationService.cancelApplication(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<LeaveApplication> getDetail(@PathVariable Long id) {
        return Result.success(leaveApplicationService.getApplicationDetail(id));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Page<LeaveApplication>> queryList(@ModelAttribute LeaveApplicationQueryDTO query) {
        return Result.success(leaveApplicationService.queryApplications(query));
    }

    @GetMapping("/my-approvals")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Page<LeaveApplication>> getMyApprovals(
            @RequestParam Long approverId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(leaveApplicationService.getMyPendingApprovals(approverId, pageNum, pageSize));
    }

    @GetMapping("/balance/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<List<LeaveBalance>> getBalance(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year) {
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        return Result.success(leaveBalanceService.getEmployeeBalances(employeeId, year));
    }

    @GetMapping("/balance/{employeeId}/{leaveType}/{year}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<LeaveBalance> getBalanceDetail(
            @PathVariable Long employeeId,
            @PathVariable Integer leaveType,
            @PathVariable Integer year) {
        return Result.success(leaveBalanceService.getBalance(employeeId, leaveType, year));
    }
}
