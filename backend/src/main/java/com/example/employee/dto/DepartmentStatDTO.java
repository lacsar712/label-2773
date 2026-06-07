package com.example.employee.dto;

import lombok.Data;

@Data
public class DepartmentStatDTO {
    private Long departmentId;
    private String departmentName;
    private Long employeeCount;
    private Long headcountLimit;
    private Double headcountUsageRate;
    private Double percentage;
}
