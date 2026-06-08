package com.example.employee.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.employee.common.Result;
import com.example.employee.entity.LeaveApprovalConfig;
import com.example.employee.service.LeaveApprovalConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave/config")
@CrossOrigin(origins = "*")
public class LeaveApprovalConfigController {

    @Autowired
    private LeaveApprovalConfigService leaveApprovalConfigService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<LeaveApprovalConfig>> list(
            @RequestParam(required = false) Integer leaveType,
            @RequestParam(required = false) Long departmentId) {
        LambdaQueryWrapper<LeaveApprovalConfig> wrapper = new LambdaQueryWrapper<>();
        if (leaveType != null) {
            wrapper.and(w -> w.eq(LeaveApprovalConfig::getLeaveType, leaveType).or().eq(LeaveApprovalConfig::getLeaveType, 0));
        }
        if (departmentId != null) {
            wrapper.and(w -> w.eq(LeaveApprovalConfig::getDepartmentId, departmentId).or().isNull(LeaveApprovalConfig::getDepartmentId));
        }
        wrapper.orderByAsc(LeaveApprovalConfig::getLeaveType, LeaveApprovalConfig::getNodeIndex);
        return Result.success(leaveApprovalConfigService.list(wrapper));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<LeaveApprovalConfig> getById(@PathVariable Long id) {
        return Result.success(leaveApprovalConfigService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<LeaveApprovalConfig> create(@RequestBody LeaveApprovalConfig config) {
        leaveApprovalConfigService.save(config);
        return Result.success(config);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<LeaveApprovalConfig> update(@RequestBody LeaveApprovalConfig config) {
        leaveApprovalConfigService.updateById(config);
        return Result.success(config);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Void> delete(@PathVariable Long id) {
        leaveApprovalConfigService.removeById(id);
        return Result.success();
    }
}
