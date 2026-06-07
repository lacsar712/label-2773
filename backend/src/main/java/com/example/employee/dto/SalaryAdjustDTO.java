package com.example.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryAdjustDTO {
    @NotNull(message = "薪资记录ID不能为空")
    private Long salaryRecordId;

    @NotBlank(message = "调整字段不能为空")
    private String fieldName;

    @NotBlank(message = "调整字段标签不能为空")
    private String fieldLabel;

    private BigDecimal oldValue;

    @NotNull(message = "调整后值不能为空")
    private BigDecimal newValue;

    private String adjustReason;
}
