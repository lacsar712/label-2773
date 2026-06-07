package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("salary_adjust_log")
public class SalaryAdjustLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long salaryRecordId;
    private Long employeeId;
    private String employeeName;
    private String fieldName;
    private String fieldLabel;
    private BigDecimal oldValue;
    private BigDecimal newValue;
    private String adjustReason;
    private Long operatorId;
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
