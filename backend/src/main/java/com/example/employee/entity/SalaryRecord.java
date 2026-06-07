package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("salary_record")
public class SalaryRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;

    @NotNull(message = "员工不能为空")
    private Long employeeId;

    private String employeeName;

    private Long departmentId;

    private String departmentName;

    private String jobLevel;

    private Long templateId;

    @NotNull(message = "薪资年份不能为空")
    private Integer salaryYear;

    @NotNull(message = "薪资月份不能为空")
    private Integer salaryMonth;

    private BigDecimal baseSalary;
    private BigDecimal postAllowance;
    private BigDecimal performanceCoefficient;
    private BigDecimal performanceBonus;
    private BigDecimal overtimePay;
    private BigDecimal otherAllowance;
    private BigDecimal socialInsurancePersonal;
    private BigDecimal socialInsuranceCompany;
    private BigDecimal housingFundPersonal;
    private BigDecimal housingFundCompany;
    private BigDecimal incomeTax;
    private BigDecimal otherDeduction;
    private BigDecimal grossSalary;
    private BigDecimal totalDeduction;
    private BigDecimal netSalary;
    private BigDecimal totalCompanyCost;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issueTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String statusName;

    @TableField(exist = false)
    private List<SalaryAdjustLog> adjustLogs;
}
