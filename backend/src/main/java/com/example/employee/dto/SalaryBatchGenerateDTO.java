package com.example.employee.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SalaryBatchGenerateDTO {
    @NotNull(message = "薪资年份不能为空")
    private Integer salaryYear;

    @NotNull(message = "薪资月份不能为空")
    private Integer salaryMonth;

    private Long departmentId;

    private List<Long> employeeIds;

    private Long defaultTemplateId;
}
