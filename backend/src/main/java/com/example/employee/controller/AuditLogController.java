package com.example.employee.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.employee.common.Result;
import com.example.employee.dto.AuditLogDetailDTO;
import com.example.employee.dto.AuditLogQueryDTO;
import com.example.employee.entity.SysAuditLog;
import com.example.employee.service.SysAuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@CrossOrigin(origins = "*")
public class AuditLogController {

    @Autowired
    private SysAuditLogService sysAuditLogService;

    @PostMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<IPage<SysAuditLog>> queryPage(@RequestBody AuditLogQueryDTO query) {
        return Result.success(sysAuditLogService.queryPage(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<AuditLogDetailDTO> getDetail(@PathVariable Long id) {
        return Result.success(sysAuditLogService.getDetail(id));
    }

    @GetMapping("/operator/{operatorId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<SysAuditLog>> getOperatorTrail(@PathVariable Long operatorId) {
        return Result.success(sysAuditLogService.getOperatorTrail(operatorId));
    }

    @GetMapping("/record/{module}/{recordId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<SysAuditLog>> getRecordHistory(
            @PathVariable String module,
            @PathVariable String recordId
    ) {
        return Result.success(sysAuditLogService.getRecordHistory(module, recordId));
    }

    @PostMapping("/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Integer> archiveOldLogs() {
        return Result.success(sysAuditLogService.archiveLogsOlderThanOneYear());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.error("审计日志为只读数据，不允许删除");
    }
}
