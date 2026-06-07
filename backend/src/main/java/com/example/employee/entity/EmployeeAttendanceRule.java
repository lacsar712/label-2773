package com.example.employee.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("employee_attendance_rule")
public class EmployeeAttendanceRule {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private Long ruleId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
