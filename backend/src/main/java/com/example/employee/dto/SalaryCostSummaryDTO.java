package com.example.employee.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryCostSummaryDTO {
    private Integer salaryYear;
    private Integer salaryMonth;
    private Long departmentId;
    private String departmentName;
    private Integer employeeCount;
    private BigDecimal totalBaseSalary;
    private BigDecimal totalPostAllowance;
    private BigDecimal totalPerformanceBonus;
    private BigDecimal totalOvertimePay;
    private BigDecimal totalGrossSalary;
    private BigDecimal totalSocialInsurancePersonal;
    private BigDecimal totalSocialInsuranceCompany;
    private BigDecimal totalHousingFundPersonal;
    private BigDecimal totalHousingFundCompany;
    private BigDecimal totalIncomeTax;
    private BigDecimal totalNetSalary;
    private BigDecimal totalCompanyCost;
    private BigDecimal avgNetSalary;
    private BigDecimal momRate;
    private BigDecimal yoyRate;
}
