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
@TableName("attendance_makeup")
public class AttendanceMakeUp {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long employeeId;

    private String employeeName;

    private Long departmentId;

    private String departmentName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate attendanceDate;

    private Integer makeupType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime makeupTime;

    private String ipAddress;

    private String location;

    private BigDecimal lng;

    private BigDecimal lat;

    private String reason;

    private Long approverId;

    private String approverName;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime approvalTime;

    private String approvalRemark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
