package com.example.employee.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SalaryCostReportDTO {
    private Integer salaryYear;
    private Integer salaryMonth;
    private BigDecimal totalCompanyCost;
    private BigDecimal totalNetSalary;
    private Integer totalEmployeeCount;
    private BigDecimal avgNetSalary;
    private BigDecimal momRate;
    private BigDecimal yoyRate;
    private List<SalaryCostSummaryDTO> departmentSummaries;
    private List<SalaryCostSummaryDTO> monthlyTrend;
}
