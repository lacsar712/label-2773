package com.example.employee.dto;

import lombok.Data;

@Data
public class SalaryRecordQueryDTO {
    private Long employeeId;
    private Long departmentId;
    private Integer salaryYear;
    private Integer salaryMonth;
    private Integer status;
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
