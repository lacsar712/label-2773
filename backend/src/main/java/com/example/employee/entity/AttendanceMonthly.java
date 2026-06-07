package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("attendance_monthly")
public class AttendanceMonthly {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private String employeeName;

    private Long departmentId;

    private String departmentName;

    private Integer statYear;

    private Integer statMonth;

    private Integer workDays;

    private Integer actualDays;

    private Integer lateCount;

    private Integer earlyCount;

    private Integer absentCount;

    private Integer exceptionCount;

    private Long totalWorkMinutes;

    private Integer totalLateMinutes;

    private Integer totalEarlyMinutes;

    private BigDecimal exceptionRate;

    private BigDecimal salaryDeduction;

    private Integer leaveDays;

    private Integer businessTripDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
