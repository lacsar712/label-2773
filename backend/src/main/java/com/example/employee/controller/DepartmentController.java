package com.example.employee.controller;

import com.example.employee.annotation.AuditLog;
import com.example.employee.common.OperationType;
import com.example.employee.common.Result;
import com.example.employee.common.TargetModule;
import com.example.employee.entity.Department;
import com.example.employee.entity.DepartmentNotification;
import com.example.employee.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "*")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @GetMapping("/tree")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> getTree() {
        return Result.success(departmentService.listWithTree());
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> getList() {
        return Result.success(departmentService.listFlat());
    }

    @GetMapping("/enabled")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> getEnabled() {
        return Result.success(departmentService.listEnabled());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Department> getById(@PathVariable Long id) {
        return Result.success(departmentService.getDetail(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Department>> search(@RequestParam String keyword) {
        return Result.success(departmentService.searchByCode(keyword));
    }

    @GetMapping("/check-code")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Boolean> checkCode(@RequestParam String code, @RequestParam(required = false) Long excludeId) {
        return Result.success(departmentService.checkCodeUnique(code, excludeId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.CREATE, recordNameField = "name")
    public Result<Boolean> create(@RequestBody @Valid Department department) {
        return Result.success(departmentService.createDepartment(department));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.UPDATE, recordNameField = "name")
    public Result<Boolean> update(@RequestBody @Valid Department department) {
        return Result.success(departmentService.updateDepartment(department));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.UPDATE)
    public Result<Boolean> toggleEnabled(@PathVariable Long id, @RequestParam Boolean enabled) {
        return Result.success(departmentService.toggleEnabled(id, enabled));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.DEPARTMENT, operation = OperationType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(departmentService.deleteDepartment(id));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<DepartmentNotification>> getNotifications() {
        return Result.success(departmentService.listNotifications());
    }
}
