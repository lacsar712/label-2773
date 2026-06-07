package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryTemplateApplyDTO {
    @NotNull(message = "模板ID不能为空")
    private Long templateId;

    private List<Long> employeeIds;

    private Long departmentId;

    @NotNull(message = "薪资年份不能为空")
    private Integer salaryYear;

    @NotNull(message = "薪资月份不能为空")
    private Integer salaryMonth;
}
