package com.example.employee.controller;

import com.example.employee.annotation.AuditLog;
import com.example.employee.common.OperationType;
import com.example.employee.common.Result;
import com.example.employee.common.TargetModule;
import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import com.example.employee.service.DepartmentService;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "*")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DepartmentService departmentService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<List<Employee>> getAll() {
        return Result.success(employeeService.listWithDepartment());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public Result<Employee> getById(@PathVariable Long id) {
        return Result.success(employeeService.getByIdWithDepartment(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.EMPLOYEE, operation = OperationType.CREATE, recordNameField = "name")
    public Result<Boolean> create(@RequestBody @Valid Employee employee) {
        validateDepartmentEnabled(employee.getDepartmentId());
        return Result.success(employeeService.save(employee));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.EMPLOYEE, operation = OperationType.UPDATE, recordNameField = "name")
    public Result<Boolean> update(@RequestBody @Valid Employee employee) {
        validateDepartmentEnabled(employee.getDepartmentId());
        return Result.success(employeeService.updateById(employee));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @AuditLog(module = TargetModule.EMPLOYEE, operation = OperationType.DELETE)
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(employeeService.removeById(id));
    }

    private void validateDepartmentEnabled(Long departmentId) {
        if (departmentId == null) {
            return;
        }
        Department dept = departmentService.getById(departmentId);
        if (dept == null) {
            throw new RuntimeException("所选部门不存在");
        }
        if (!dept.getEnabled()) {
            throw new RuntimeException("所选部门已停用，不可关联员工");
        }
    }
}
