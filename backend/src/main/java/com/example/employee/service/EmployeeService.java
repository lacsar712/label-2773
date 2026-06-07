package com.example.employee.service;

import com.example.employee.entity.Department;
import com.example.employee.entity.Employee;
import com.example.employee.mapper.EmployeeMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmployeeService extends ServiceImpl<EmployeeMapper, Employee> {

    @Autowired
    private DepartmentService departmentService;

    public List<Employee> listWithDepartment() {
        List<Employee> employees = list();
        enrichEmployees(employees);
        return employees;
    }

    public Employee getByIdWithDepartment(Long id) {
        Employee employee = getById(id);
        if (employee != null) {
            enrichEmployees(List.of(employee));
        }
        return employee;
    }

    private void enrichEmployees(List<Employee> employees) {
        List<Department> departments = departmentService.list();
        Map<Long, String> deptMap = departments.stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        for (Employee emp : employees) {
            if (emp.getDepartmentId() != null) {
                emp.setDepartmentName(deptMap.get(emp.getDepartmentId()));
            }
        }
    }
}
