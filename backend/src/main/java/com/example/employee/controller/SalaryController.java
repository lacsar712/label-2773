package com.example.employee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.employee.annotation.AuditLog;
import com.example.employee.common.OperationType;
import com.example.employee.common.Result;
import com.example.employee.common.TargetModule;
import com.example.employee.context.UserContext;
import com.example.employee.dto.*;
import com.example.employee.entity.SalaryRecord;
import com.example.employee.entity.SalaryAdjustLog;
import com.example.employee.entity.SalaryTemplate;
import com.example.employee.service.SalaryAdjustLogService;
import com.example.employee.service.SalaryRecordService;
import com.example.employee.service.SalaryTemplateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salary")
@CrossOrigin(origins = "*")
public class SalaryController {

    @Autowired
    private SalaryTemplateService salaryTemplateService;

    @Autowired
    private SalaryRecordService salaryRecordService;

    @Autowired
    private SalaryAdjustLogService salaryAdjustLogService;

    // ==================== 薪资模板管理 ====================

    @GetMapping("/template/list")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<SalaryTemplate>> listTemplates(@RequestParam(required = false) String keyword) {
        return Result.success(salaryTemplateService.listAll(keyword));
    }

    @GetMapping("/template/enabled")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<List<SalaryTemplate>> listEnabledTemplates() {
        return Result.success(salaryTemplateService.listEnabled());
    }

    @GetMapping("/template/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<SalaryTemplate> getTemplate(@PathVariable Long id) {
        return Result.success(salaryTemplateService.getById(id));
    }

    @PostMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.CREATE, recordNameField = "templateName")
    public Result<SalaryTemplate> createTemplate(@RequestBody @Valid SalaryTemplate template) {
        return Result.success(salaryTemplateService.createTemplate(template));
    }

    @PutMapping("/template")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE, recordNameField = "templateName")
    public Result<SalaryTemplate> updateTemplate(@RequestBody @Valid SalaryTemplate template) {
        return Result.success(salaryTemplateService.updateTemplate(template));
    }

    @PostMapping("/template/toggle/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE)
    public Result<Void> toggleTemplate(@PathVariable Long id) {
        salaryTemplateService.toggleEnabled(id);
        return Result.success(null);
    }

    // ==================== 薪资记录管理 ====================

    @PostMapping("/batch-generate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.CREATE)
    public Result<List<SalaryRecord>> batchGenerate(@RequestBody @Valid SalaryBatchGenerateDTO dto) {
        return Result.success(salaryRecordService.batchGenerate(dto));
    }

    @PostMapping("/apply-template")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE)
    public Result<Void> applyTemplate(@RequestBody @Valid SalaryTemplateApplyDTO dto) {
        salaryRecordService.applyTemplate(dto);
        return Result.success(null);
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Page<SalaryRecord>> queryRecords(@ModelAttribute SalaryRecordQueryDTO query) {
        return Result.success(salaryRecordService.queryRecords(query, false));
    }

    @GetMapping("/record/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<SalaryRecord> getRecordDetail(@PathVariable Long id) {
        return Result.success(salaryRecordService.getDetail(id, false));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE, recordNameField = "employeeName")
    public Result<SalaryRecord> adjustField(@RequestBody @Valid SalaryAdjustDTO dto) {
        UserInfoDTO user = UserContext.getCurrentUser();
        Long operatorId = user != null ? user.getUserId() : null;
        String operatorName = user != null ? user.getNickname() : null;
        return Result.success(salaryRecordService.adjustField(dto, operatorId, operatorName));
    }

    @PostMapping("/confirm/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE)
    public Result<SalaryRecord> confirmRecord(@PathVariable Long id) {
        return Result.success(salaryRecordService.confirmRecord(id));
    }

    @PostMapping("/batch-confirm")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE)
    public Result<List<SalaryRecord>> batchConfirm(@RequestBody List<Long> ids) {
        return Result.success(salaryRecordService.batchConfirm(ids));
    }

    @PostMapping("/issue/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE)
    public Result<SalaryRecord> issueRecord(@PathVariable Long id) {
        return Result.success(salaryRecordService.issueRecord(id));
    }

    @PostMapping("/batch-issue")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @AuditLog(module = TargetModule.SALARY, operation = OperationType.UPDATE)
    public Result<List<SalaryRecord>> batchIssue(@RequestBody List<Long> ids) {
        return Result.success(salaryRecordService.batchIssue(ids));
    }

    @GetMapping("/adjust-logs/{salaryRecordId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<SalaryAdjustLog>> getAdjustLogs(@PathVariable Long salaryRecordId) {
        return Result.success(salaryAdjustLogService.listBySalaryRecordId(salaryRecordId));
    }

    @GetMapping("/field-labels")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<List<Map<String, Object>>> getFieldLabels() {
        return Result.success(salaryRecordService.getFieldLabels());
    }

    // ==================== 员工端接口 ====================

    @GetMapping("/my-records")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<Page<SalaryRecord>> getMyRecords(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user == null || user.getEmployeeId() == null) {
            return Result.success(new Page<>(pageNum, pageSize));
        }
        return Result.success(salaryRecordService.getMyRecords(user.getEmployeeId(), pageNum, pageSize));
    }

    @GetMapping("/my-record/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public Result<SalaryRecord> getMyRecordDetail(@PathVariable Long id) {
        UserInfoDTO user = UserContext.getCurrentUser();
        if (user == null || user.getEmployeeId() == null) {
            throw new RuntimeException("用户信息异常");
        }
        return Result.success(salaryRecordService.getMyRecordDetail(user.getEmployeeId(), id));
    }

    // ==================== 人力成本报表 ====================

    @GetMapping("/cost-report")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<SalaryCostReportDTO> getCostReport(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Long departmentId) {
        return Result.success(salaryRecordService.getCostReport(year, month, departmentId));
    }
}
