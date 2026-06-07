package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("salary_template")
public class SalaryTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @NotBlank(message = "适用职级不能为空")
    private String jobLevel;

    @NotNull(message = "基本工资不能为空")
    private BigDecimal baseSalary;

    private BigDecimal postAllowance;

    private BigDecimal performanceCoefficient;

    private BigDecimal performanceBonus;

    private BigDecimal socialInsurancePersonalRate;

    private BigDecimal socialInsuranceCompanyRate;

    private BigDecimal housingFundPersonalRate;

    private BigDecimal housingFundCompanyRate;

    private BigDecimal socialInsuranceBase;

    private String description;

    private Integer enabled;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
