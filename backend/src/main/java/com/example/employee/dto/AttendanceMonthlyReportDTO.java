package com.example.employee.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AttendanceMonthlyReportDTO {
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

    private BigDecimal exceptionRate;
}
