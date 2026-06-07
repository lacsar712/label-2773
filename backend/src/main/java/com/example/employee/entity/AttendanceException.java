package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("attendance_exception")
public class AttendanceException {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private String employeeName;

    private Long departmentId;

    private String departmentName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate attendanceDate;

    private Integer exceptionType;

    private Integer lateMinutes;

    private Integer earlyMinutes;

    private Long recordId;

    private BigDecimal salaryDeduction;

    private Long deductionRuleId;

    private Integer handled;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
