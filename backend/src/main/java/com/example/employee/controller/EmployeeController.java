package com.example.employee.controller;

import com.example.employee.common.Result;
import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import com.example.employee.service.DepartmentService;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    public Result<List<Employee>> getAll() {
        return Result.success(employeeService.listWithDepartment());
    }

    @GetMapping("/{id}")
    public Result<Employee> getById(@PathVariable Long id) {
        return Result.success(employeeService.getByIdWithDepartment(id));
    }

    @PostMapping
    public Result<Boolean> create(@RequestBody @Valid Employee employee) {
        validateDepartmentEnabled(employee.getDepartmentId());
        return Result.success(employeeService.save(employee));
    }

    @PutMapping
    public Result<Boolean> update(@RequestBody @Valid Employee employee) {
        validateDepartmentEnabled(employee.getDepartmentId());
        return Result.success(employeeService.updateById(employee));
    }

    @DeleteMapping("/{id}")
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
