package com.example.employee.dto;

import com.example.employee.entity.Department;
import com.example.employee.entity.DepartmentLeaderChangeHistory;
import com.example.employee.entity.Employee;
import lombok.Data;

import java.util.List;

@Data
public class DepartmentDetailDTO {
    private Department department;

    private List<Employee> activeEmployees;

    private List<Department> subDepartments;

    private List<DepartmentLeaderChangeHistory> leaderChangeHistory;
}
